package com.kelseyde.calvin.board;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Represents the current state of the chess board, including the positions of the pieces, the side to move, en passant
 * rights, fifty-move counter, and the move counter. Includes functions to 'make' and 'unmake' moves on the board, which
 * are fundamental to both the search and evaluation algorithms. Uses bitboards to represent the pieces and 'toggling'
 * functions to set and unset pieces.
 *
 * @see <a href="https://www.chessprogramming.org/Board_Representation">Chess Programming Wiki</a>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Board {

    long pawns =        Bits.WHITE_PAWNS_START | Bits.BLACK_PAWNS_START;
    long knights =      Bits.WHITE_KNIGHTS_START | Bits.BLACK_KNIGHTS_START;
    long bishops =      Bits.WHITE_BISHOPS_START | Bits.BLACK_BISHOPS_START;
    long rooks =        Bits.WHITE_ROOKS_START | Bits.BLACK_ROOKS_START;
    long queens =       Bits.WHITE_QUEENS_START | Bits.BLACK_QUEENS_START;
    long kings =        Bits.WHITE_KING_START | Bits.BLACK_KING_START;

    long whitePieces =  Bits.WHITE_PIECES_START;
    long blackPieces =  Bits.BLACK_PIECES_START;
    long occupied =     Bits.PIECES_START;

    Piece[] pieceList = Bits.getStartingPieceList();

    boolean whiteToMove = true;

    GameState gameState = new GameState();
    Deque<GameState> gameStateHistory = new ArrayDeque<>();
    Deque<Move> moveHistory = new ArrayDeque<>();

    public Board() {
        gameState.setZobrist(Zobrist.generateKey(this));
        gameState.setPawnZobrist(Zobrist.generatePawnKey(this));
    }

    /**
     * Updates the internal board representation with the {@link Move} just made. Toggles the piece bitboards to move the
     * piece + remove the captured piece, plus special rules for pawn double-moves, castling, promotion and en passant.
     */
    public boolean makeMove(Move move) {

        int startSquare = move.getFrom();
        int endSquare = move.getTo();
        Piece piece = pieceList[startSquare];
        if (piece == null) return false;
        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : pieceList[endSquare];
        gameStateHistory.push(gameState.copy());

        if (move.isPawnDoubleMove())  makePawnDoubleMove(startSquare, endSquare);
        else if (move.isCastling())   makeCastleMove(startSquare, endSquare);
        else if (move.isPromotion())  makePromotionMove(startSquare, endSquare, move.getPromotionPiece(), capturedPiece);
        else if (move.isEnPassant())  makeEnPassantMove(startSquare, endSquare);
        else                          makeStandardMove(startSquare, endSquare, piece, capturedPiece);

        updateGameState(startSquare, endSquare, piece, capturedPiece, move);
        moveHistory.push(move);
        whiteToMove = !whiteToMove;
        return true;

    }

    /**
     * Reverts the internal board representation to the state before the last move. Toggles the piece bitboards to move the
     * piece + reinstate the captured piece, plus special rules for pawn double-moves, castling, promotion and en passant.
     */
    public void unmakeMove() {

        whiteToMove = !whiteToMove;
        Move move = moveHistory.pop();
        int startSquare = move.getFrom();
        int endSquare = move.getTo();
        Piece piece = pieceAt(endSquare);

        if (move.isCastling())        unmakeCastlingMove(startSquare, endSquare);
        else if (move.isPromotion())  unmakePromotionMove(startSquare, endSquare, move.getPromotionPiece());
        else if (move.isEnPassant())  unmakeEnPassantMove(startSquare, endSquare);
        else                          unmakeStandardMove(startSquare, endSquare, piece);

        gameState = gameStateHistory.pop();

    }

    private void makePawnDoubleMove(int startSquare, int endSquare) {
        toggleSquares(Piece.PAWN, whiteToMove, startSquare, endSquare);
        pieceList[startSquare] = null;
        pieceList[endSquare] = Piece.PAWN;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, startSquare, endSquare, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, startSquare, endSquare, Piece.PAWN, whiteToMove);
    }

    private void makeCastleMove(int startSquare, int endSquare) {
        toggleSquares(Piece.KING, whiteToMove, startSquare, endSquare);
        pieceList[startSquare] = null;
        pieceList[endSquare] = Piece.KING;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, startSquare, endSquare, Piece.KING, whiteToMove);
        boolean isKingside = Board.file(endSquare) == 6;
        int rookStartSquare;
        int rookEndSquare;
        if (isKingside) {
            rookStartSquare = whiteToMove ? 7 : 63;
            rookEndSquare = whiteToMove ? 5 : 61;
        } else {
            rookStartSquare = whiteToMove ? 0 : 56;
            rookEndSquare = whiteToMove ? 3 : 59;
        }
        toggleSquares(Piece.ROOK, whiteToMove, rookStartSquare, rookEndSquare);
        pieceList[rookStartSquare] = null;
        pieceList[rookEndSquare] = Piece.ROOK;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, rookStartSquare, rookEndSquare, Piece.ROOK, whiteToMove);
    }

    private void makeEnPassantMove(int startSquare, int endSquare) {
        toggleSquares(Piece.PAWN, whiteToMove, startSquare, endSquare);
        int pawnSquare = whiteToMove ? endSquare - 8 : endSquare + 8;
        toggleSquare(Piece.PAWN, !whiteToMove, pawnSquare);
        pieceList[startSquare] = null;
        pieceList[pawnSquare] = null;
        pieceList[endSquare] = Piece.PAWN;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, startSquare, Piece.PAWN, whiteToMove);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, pawnSquare, Piece.PAWN, !whiteToMove);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, endSquare, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, startSquare, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, pawnSquare, Piece.PAWN, !whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, endSquare, Piece.PAWN, whiteToMove);
    }

    private void makePromotionMove(int startSquare, int endSquare, Piece promotionPiece, Piece capturedPiece) {
        toggleSquare(Piece.PAWN, whiteToMove, startSquare);
        toggleSquare(promotionPiece, whiteToMove, endSquare);
        pieceList[startSquare] = null;
        pieceList[endSquare] = promotionPiece;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, startSquare, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, startSquare, Piece.PAWN, whiteToMove);
        if (capturedPiece != null) {
            toggleSquare(capturedPiece, !whiteToMove, endSquare);
            gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, endSquare, capturedPiece, !whiteToMove);
            if (capturedPiece == Piece.PAWN) {
                gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, endSquare, capturedPiece, !whiteToMove);
            }
        }
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, endSquare, promotionPiece, whiteToMove);
    }

    private void makeStandardMove(int startSquare, int endSquare, Piece piece, Piece capturedPiece) {
        toggleSquares(piece, whiteToMove, startSquare, endSquare);
        if (capturedPiece != null) {
            toggleSquare(capturedPiece, !whiteToMove, endSquare);
            gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, endSquare, capturedPiece, !whiteToMove);
            if (capturedPiece == Piece.PAWN) {
                gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, endSquare, capturedPiece, !whiteToMove);
            }
        }
        pieceList[startSquare] = null;
        pieceList[endSquare] = piece;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, startSquare, endSquare, piece, whiteToMove);
        if (piece == Piece.PAWN) {
            gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, startSquare, endSquare, piece, whiteToMove);
        }
    }

    private void updateGameState(int startSquare, int endSquare, Piece piece, Piece capturedPiece, Move move) {
        gameState.capturedPiece = capturedPiece;
        boolean resetClock = capturedPiece != null || Piece.PAWN.equals(piece);
        gameState.halfMoveClock = resetClock ? 0 : ++gameState.halfMoveClock;

        int castlingRights = calculateCastlingRights(startSquare, endSquare, piece);
        gameState.zobrist = Zobrist.updateCastlingRights(gameState.zobrist, gameState.castlingRights, castlingRights);
        gameState.castlingRights = castlingRights;

        int enPassantFile = move.isPawnDoubleMove() ? Board.file(endSquare) : -1;
        gameState.zobrist = Zobrist.updateEnPassantFile(gameState.zobrist, gameState.enPassantFile, enPassantFile);
        gameState.enPassantFile = enPassantFile;

        gameState.zobrist = Zobrist.updateSideToMove(gameState.zobrist);
    }

    private void unmakeCastlingMove(int startSquare, int endSquare) {
        toggleSquares(Piece.KING, whiteToMove, endSquare, startSquare);
        boolean isKingside = Board.file(endSquare) == 6;
        int rookStartSquare;
        int rookEndSquare;
        if (isKingside) {
            rookStartSquare = whiteToMove ? 5 : 61;
            rookEndSquare = whiteToMove ? 7 : 63;
        } else {
            rookStartSquare = whiteToMove ? 3 : 59;
            rookEndSquare = whiteToMove ? 0 : 56;
        }
        toggleSquares(Piece.ROOK, whiteToMove, rookStartSquare, rookEndSquare);
        pieceList[startSquare] = Piece.KING;
        pieceList[endSquare] = null;
        pieceList[rookEndSquare] = Piece.ROOK;
        pieceList[rookStartSquare] = null;
    }

    private void unmakePromotionMove(int startSquare, int endSquare, Piece promotionPiece) {
        toggleSquare(promotionPiece, whiteToMove, endSquare);
        toggleSquare(Piece.PAWN, whiteToMove, startSquare);
        if (gameState.getCapturedPiece() != null) {
            toggleSquare(gameState.getCapturedPiece(), !whiteToMove, endSquare);
        }
        pieceList[startSquare] = Piece.PAWN;
        pieceList[endSquare] = gameState.getCapturedPiece() != null ? gameState.getCapturedPiece() : null;
    }

    private void unmakeEnPassantMove(int startSquare, int endSquare) {
        toggleSquares(Piece.PAWN, whiteToMove, endSquare, startSquare);
        int captureSquare = whiteToMove ? endSquare - 8 : endSquare + 8;
        toggleSquare(Piece.PAWN, !whiteToMove, captureSquare);
        pieceList[startSquare] = Piece.PAWN;
        pieceList[endSquare] = null;
        pieceList[captureSquare] = Piece.PAWN;
    }

    private void unmakeStandardMove(int startSquare, int endSquare, Piece piece) {
        toggleSquares(piece, whiteToMove, endSquare, startSquare);
        if (gameState.getCapturedPiece() != null) {
            toggleSquare(gameState.getCapturedPiece(), !whiteToMove, endSquare);
        }
        pieceList[startSquare] = piece;
        pieceList[endSquare] = gameState.getCapturedPiece() != null ? gameState.getCapturedPiece() : null;
    }

    /**
     * Make a 'null' move, meaning the side to move passes their turn and gives the opponent a double-move. Used exclusively
     * during null-move pruning during search.
     */
    public void makeNullMove() {
        whiteToMove = !whiteToMove;
        long newZobristKey = Zobrist.updateKeyAfterNullMove(gameState.getZobrist(), gameState.getEnPassantFile());
        GameState newGameState = new GameState(newZobristKey, gameState.getPawnZobrist(), null, -1, gameState.getCastlingRights(), 0);
        gameStateHistory.push(gameState);
        gameState = newGameState;
    }

    /**
     * Unmake the 'null' move used during null-move pruning to try and prove a beta cut-off.
     */
    public void unmakeNullMove() {
        whiteToMove = !whiteToMove;
        gameState = gameStateHistory.pop();
    }

    public void toggleSquares(Piece type, boolean white, int startSquare, int endSquare) {
        long toggleMask = (1L << startSquare | 1L << endSquare);
        toggle(type, white, toggleMask);
    }

    public void toggleSquare(Piece type, boolean white, int square) {
        long toggleMask = 1L << square;
        toggle(type, white, toggleMask);
    }

    private void toggle(Piece type, boolean white, long toggleMask) {
        switch (type) {
            case PAWN ->    pawns ^= toggleMask;
            case KNIGHT ->  knights ^= toggleMask;
            case BISHOP ->  bishops ^= toggleMask;
            case ROOK ->    rooks ^= toggleMask;
            case QUEEN ->   queens ^= toggleMask;
            case KING ->    kings ^= toggleMask;
        }
        if (white) {
            whitePieces ^= toggleMask;
        } else {
            blackPieces ^= toggleMask;
        }
        occupied ^= toggleMask;
    }

    public void removeKing(boolean white) {
        long toggleMask = white ? (kings & whitePieces) : (kings & blackPieces);
        kings ^= toggleMask;
        if (white) {
            whitePieces ^= toggleMask;
        } else {
            blackPieces ^= toggleMask;
        }
        occupied ^= toggleMask;
    }

    public void addKing(int kingSquare, boolean white) {
        long toggleMask = 1L << kingSquare;
        kings |= toggleMask;
        if (white) {
            whitePieces |= toggleMask;
        } else {
            blackPieces |= toggleMask;
        }
        occupied |= toggleMask;
    }

    private int calculateCastlingRights(int startSquare, int endSquare, Piece pieceType) {
        int newCastlingRights = gameState.getCastlingRights();
        if (newCastlingRights == 0b0000) {
            // Both sides already lost castling rights, so nothing to calculate.
            return newCastlingRights;
        }
        // Any move by the king removes castling rights.
        if (Piece.KING.equals(pieceType)) {
            newCastlingRights &= whiteToMove ? Bits.CLEAR_WHITE_CASTLING_MASK : Bits.CLEAR_BLACK_CASTLING_MASK;
        }
        // Any move starting from/ending at a rook square removes castling rights for that corner.
        // Note: all of these cases need to be checked, to cover the scenario where a rook in starting position captures
        // another rook in starting position; in that case, both sides lose castling rights!
        if (startSquare == 7 || endSquare == 7) {
            newCastlingRights &= Bits.CLEAR_WHITE_KINGSIDE_MASK;
        }
        if (startSquare == 63 || endSquare == 63) {
            newCastlingRights &= Bits.CLEAR_BLACK_KINGSIDE_MASK;
        }
        if (startSquare == 0 || endSquare == 0) {
            newCastlingRights &= Bits.CLEAR_WHITE_QUEENSIDE_MASK;
        }
        if (startSquare == 56 || endSquare == 56) {
            newCastlingRights &= Bits.CLEAR_BLACK_QUEENSIDE_MASK;
        }
        return newCastlingRights;
    }

    public Piece pieceAt(int square) {
        return pieceList[square];
    }

    public long getPawns(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return pawns & side;
    }

    public long getKnights(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return knights & side;
    }

    public long getBishops(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return bishops & side;
    }

    public long getRooks(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return rooks & side;
    }

    public long getQueens(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return queens & side;
    }

    public long getKing(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return kings & side;
    }

    public long getPieces(boolean white) {
        return white ? whitePieces : blackPieces;
    }

    public int countPieces() {
        return Bitwise.countBits(occupied);
    }

    public boolean hasPiecesRemaining(boolean white) {
        return white ?
                (getKnights(true) != 0 || getBishops(true) != 0 || getRooks(true) != 0 || getQueens(true) != 0) :
                (getKnights(false) != 0 || getBishops(false) != 0 || getRooks(false) != 0 || getQueens(false) != 0);
    }

    public boolean isPawnEndgame() {
        return (pawns != 0) && knights == 0 && bishops == 0 && rooks == 0 && queens == 0;
    }

    public static int file(int sq) {
        return sq & 0b000111;
    }

    public static int rank(int sq) {
        return sq >>> 3;
    }

    public static int colourIndex(boolean white) {
        return white ? 1 : 0;
    }

    public static int squareIndex(int rank, int file) {
        return 8 * rank + file;
    }

    public static boolean isValidIndex(int square) {
        return square >= 0 && square < 64;
    }

    public Board copy() {
        Board newBoard = new Board();
        newBoard.setPawns(this.getPawns());
        newBoard.setKnights(this.getKnights());
        newBoard.setBishops(this.getBishops());
        newBoard.setRooks(this.getRooks());
        newBoard.setQueens(this.getQueens());
        newBoard.setKings(this.getKings());
        newBoard.setWhitePieces(this.getWhitePieces());
        newBoard.setBlackPieces(this.getBlackPieces());
        newBoard.setOccupied(this.getOccupied());
        newBoard.setWhiteToMove(this.isWhiteToMove());
        newBoard.setGameState(this.getGameState().copy());
        Deque<GameState> gameStateHistory = new ArrayDeque<>();
        this.getGameStateHistory().forEach(gameState -> gameStateHistory.add(gameState.copy()));
        newBoard.setGameStateHistory(gameStateHistory);
        Deque<Move> moveHistory = new ArrayDeque<>();
        this.getMoveHistory().forEach(move -> moveHistory.add(new Move(move.value())));
        newBoard.setMoveHistory(moveHistory);
        newBoard.setPieceList(Arrays.copyOf(this.getPieceList(), this.getPieceList().length));
        return newBoard;
    }

}
