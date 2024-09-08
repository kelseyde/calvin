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

        int from = move.getFrom();
        int to = move.getTo();
        Piece piece = pieceList[from];
        if (piece == null) return false;
        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : pieceList[to];
        gameStateHistory.push(gameState.copy());

        if (move.isPawnDoubleMove())  makePawnDoubleMove(from, to);
        else if (move.isCastling())   makeCastleMove(from, to);
        else if (move.isPromotion())  makePromotionMove(from, to, move.getPromotionPiece(), capturedPiece);
        else if (move.isEnPassant())  makeEnPassantMove(from, to);
        else                          makeStandardMove(from, to, piece, capturedPiece);

        updateGameState(from, to, piece, capturedPiece, move);
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
        int from = move.getFrom();
        int to = move.getTo();
        Piece piece = pieceAt(to);

        if (move.isCastling())        unmakeCastlingMove(from, to);
        else if (move.isPromotion())  unmakePromotionMove(from, to, move.getPromotionPiece());
        else if (move.isEnPassant())  unmakeEnPassantMove(from, to);
        else                          unmakeStandardMove(from, to, piece);

        gameState = gameStateHistory.pop();

    }

    private void makePawnDoubleMove(int from, int to) {
        toggleSquares(Piece.PAWN, whiteToMove, from, to);
        toggleMailbox(Piece.PAWN, from, to);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, from, to, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, from, to, Piece.PAWN, whiteToMove);
    }

    private void makeCastleMove(int from, int to) {
        toggleSquares(Piece.KING, whiteToMove, from, to);
        toggleMailbox(Piece.KING, from, to);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, from, to, Piece.KING, whiteToMove);
        boolean isKingside = Board.file(to) == 6;
        int rookFrom, rookTo;
        if (isKingside) {
            rookFrom = whiteToMove ? 7 : 63;
            rookTo = whiteToMove ? 5 : 61;
        } else {
            rookFrom = whiteToMove ? 0 : 56;
            rookTo = whiteToMove ? 3 : 59;
        }
        toggleSquares(Piece.ROOK, whiteToMove, rookFrom, rookTo);
        toggleMailbox(Piece.ROOK, rookFrom, rookTo);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, rookFrom, rookTo, Piece.ROOK, whiteToMove);
    }

    private void makeEnPassantMove(int from, int to) {
        toggleSquares(Piece.PAWN, whiteToMove, from, to);
        int enPassantSquare = whiteToMove ? to - 8 : to + 8;
        toggleSquare(Piece.PAWN, !whiteToMove, enPassantSquare);
        toggleMailbox(Piece.PAWN, from, to);
        pieceList[enPassantSquare] = null;
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, from, Piece.PAWN, whiteToMove);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, enPassantSquare, Piece.PAWN, !whiteToMove);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, to, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, from, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, enPassantSquare, Piece.PAWN, !whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, to, Piece.PAWN, whiteToMove);
    }

    private void makePromotionMove(int from, int to, Piece promotionPiece, Piece capturedPiece) {
        toggleSquare(Piece.PAWN, whiteToMove, from);
        toggleSquare(promotionPiece, whiteToMove, to);
        toggleMailbox(promotionPiece, from, to);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, from, Piece.PAWN, whiteToMove);
        gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, from, Piece.PAWN, whiteToMove);
        if (capturedPiece != null) {
            toggleSquare(capturedPiece, !whiteToMove, to);
            gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, to, capturedPiece, !whiteToMove);
            if (capturedPiece == Piece.PAWN) {
                gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, to, capturedPiece, !whiteToMove);
            }
        }
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, to, promotionPiece, whiteToMove);
    }

    private void makeStandardMove(int from, int to, Piece piece, Piece capturedPiece) {
        toggleSquares(piece, whiteToMove, from, to);
        if (capturedPiece != null) {
            toggleSquare(capturedPiece, !whiteToMove, to);
            gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, to, capturedPiece, !whiteToMove);
            if (capturedPiece == Piece.PAWN) {
                gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, to, capturedPiece, !whiteToMove);
            }
        }
        toggleMailbox(piece, from, to);
        gameState.zobrist = Zobrist.updatePiece(gameState.zobrist, from, to, piece, whiteToMove);
        if (piece == Piece.PAWN) {
            gameState.pawnZobrist = Zobrist.updatePiece(gameState.pawnZobrist, from, to, piece, whiteToMove);
        }
    }

    private void updateGameState(int from, int to, Piece piece, Piece capturedPiece, Move move) {
        gameState.capturedPiece = capturedPiece;
        boolean resetClock = capturedPiece != null || Piece.PAWN.equals(piece);
        gameState.halfMoveClock = resetClock ? 0 : ++gameState.halfMoveClock;

        int castlingRights = calculateCastlingRights(from, to, piece);
        gameState.zobrist = Zobrist.updateCastlingRights(gameState.zobrist, gameState.castlingRights, castlingRights);
        gameState.castlingRights = castlingRights;

        int enPassantFile = move.isPawnDoubleMove() ? Board.file(to) : -1;
        gameState.zobrist = Zobrist.updateEnPassantFile(gameState.zobrist, gameState.enPassantFile, enPassantFile);
        gameState.enPassantFile = enPassantFile;

        gameState.zobrist = Zobrist.updateSideToMove(gameState.zobrist);
    }

    private void unmakeCastlingMove(int from, int to) {
        toggleSquares(Piece.KING, whiteToMove, to, from);
        boolean isKingside = Board.file(to) == 6;
        int rookFrom, rookTo;
        if (isKingside) {
            rookFrom = whiteToMove ? 5 : 61;
            rookTo = whiteToMove ? 7 : 63;
        } else {
            rookFrom = whiteToMove ? 3 : 59;
            rookTo = whiteToMove ? 0 : 56;
        }
        toggleSquares(Piece.ROOK, whiteToMove, rookFrom, rookTo);
        pieceList[from] = Piece.KING;
        pieceList[to] = null;
        pieceList[rookTo] = Piece.ROOK;
        pieceList[rookFrom] = null;
    }

    private void unmakePromotionMove(int from, int to, Piece promotionPiece) {
        toggleSquare(promotionPiece, whiteToMove, to);
        toggleSquare(Piece.PAWN, whiteToMove, from);
        if (gameState.getCapturedPiece() != null) {
            toggleSquare(gameState.getCapturedPiece(), !whiteToMove, to);
        }
        pieceList[from] = Piece.PAWN;
        pieceList[to] = gameState.getCapturedPiece() != null ? gameState.getCapturedPiece() : null;
    }

    private void unmakeEnPassantMove(int from, int to) {
        toggleSquares(Piece.PAWN, whiteToMove, to, from);
        int captureSquare = whiteToMove ? to - 8 : to + 8;
        toggleSquare(Piece.PAWN, !whiteToMove, captureSquare);
        pieceList[from] = Piece.PAWN;
        pieceList[to] = null;
        pieceList[captureSquare] = Piece.PAWN;
    }

    private void unmakeStandardMove(int from, int to, Piece piece) {
        toggleSquares(piece, whiteToMove, to, from);
        if (gameState.getCapturedPiece() != null) {
            toggleSquare(gameState.getCapturedPiece(), !whiteToMove, to);
        }
        pieceList[from] = piece;
        pieceList[to] = gameState.getCapturedPiece() != null ? gameState.getCapturedPiece() : null;
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

    public void toggleSquares(Piece type, boolean white, int from, int to) {
        long toggleMask = (1L << from | 1L << to);
        toggle(type, white, toggleMask);
    }

    public void toggleSquare(Piece type, boolean white, int square) {
        long toggleMask = 1L << square;
        toggle(type, white, toggleMask);
    }

    private void toggleMailbox(Piece type, int from, int to) {
        pieceList[from] = null;
        pieceList[to] = type;
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

    private int calculateCastlingRights(int from, int to, Piece pieceType) {
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
        if (from == 7 || to == 7) {
            newCastlingRights &= Bits.CLEAR_WHITE_KINGSIDE_MASK;
        }
        if (from == 63 || to == 63) {
            newCastlingRights &= Bits.CLEAR_BLACK_KINGSIDE_MASK;
        }
        if (from == 0 || to == 0) {
            newCastlingRights &= Bits.CLEAR_WHITE_QUEENSIDE_MASK;
        }
        if (from == 56 || to == 56) {
            newCastlingRights &= Bits.CLEAR_BLACK_QUEENSIDE_MASK;
        }
        return newCastlingRights;
    }

    public Piece pieceAt(int square) {
        return pieceList[square];
    }

    public long pawns() {
        return pawns;
    }

    public long pawns(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return pawns & side;
    }

    public long knights() {
        return knights;
    }

    public long knights(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return knights & side;
    }

    public long bishops() {
        return bishops;
    }

    public long bishops(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return bishops & side;
    }

    public long rooks() {
        return rooks;
    }

    public long rooks(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return rooks & side;
    }

    public long queens() {
        return queens;
    }

    public long queens(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return queens & side;
    }

    public long kings() {
        return kings;
    }

    public long king(boolean white) {
        long side = white ? whitePieces : blackPieces;
        return kings & side;
    }

    public long pieces(boolean white) {
        return white ? whitePieces : blackPieces;
    }

    public int countPieces() {
        return Bitwise.countBits(occupied);
    }

    public boolean isCapture(Move move) {
        return move.isEnPassant() || pieceList[move.getTo()] != null;
    }

    public long key() {
        return gameState.getZobrist();
    }

    public boolean hasPiecesRemaining(boolean white) {
        return white ?
                (knights(true) != 0 || bishops(true) != 0 || rooks(true) != 0 || queens(true) != 0) :
                (knights(false) != 0 || bishops(false) != 0 || rooks(false) != 0 || queens(false) != 0);
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
        newBoard.setPawns(this.pawns());
        newBoard.setKnights(this.knights());
        newBoard.setBishops(this.bishops());
        newBoard.setRooks(this.rooks());
        newBoard.setQueens(this.queens());
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
