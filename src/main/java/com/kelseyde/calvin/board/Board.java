package com.kelseyde.calvin.board;

import com.kelseyde.calvin.board.Bits.Castling;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.utils.notation.FEN;

import java.util.Arrays;

/**
 * Represents the current state of the chess board, including the positions of the pieces, the side to move, en passant
 * rights, fifty-move counter, and the move counter. Includes functions to 'make' and 'unmake' moves on the board, which
 * are fundamental to both the search and evaluation algorithms. Uses bitboards to represent the pieces and 'toggling'
 * functions to set and unset pieces.
 *
 * @see <a href="https://www.chessprogramming.org/Board_Representation">Chess Programming Wiki</a>
 */
public class Board {

    private long pawns;
    private long knights;
    private long bishops;
    private long rooks;
    private long queens;
    private long kings;
    private long whitePieces;
    private long blackPieces;
    private long occupied;

    private Piece[] pieces;
    private GameState state;
    private GameState[] states;
    private Move[] moves;
    private boolean white;
    private int ply;

    public Board() {
        this.pawns       = 0L;
        this.knights     = 0L;
        this.bishops     = 0L;
        this.rooks       = 0L;
        this.queens      = 0L;
        this.kings       = 0L;
        this.whitePieces = 0L;
        this.blackPieces = 0L;
        this.occupied    = 0L;
        this.pieces      = new Piece[Square.COUNT];
        this.moves       = new Move[Search.MAX_DEPTH];
        this.states      = new GameState[Search.MAX_DEPTH];
        this.state       = new GameState();
        this.white       = true;
        this.ply         = 0;
    }

    /**
     * Updates the internal board representation with the {@link Move} just made. Toggles the piece bitboards to move the
     * piece + remove the captured piece, plus special rules for pawn double-moves, castling, promotion and en passant.
     */
    public boolean makeMove(Move move) {

        final int from = move.from();
        final int to = move.to();
        final Piece piece = pieces[from];
        if (piece == null) return false;
        final Piece captured = move.isEnPassant() ? Piece.PAWN : pieces[to];
        states[ply] = state.copy();

        if (move.isPawnDoubleMove())  makePawnDoubleMove(from, to);
        else if (move.isCastling())   makeCastleMove(from, to);
        else if (move.isPromotion())  makePromotionMove(from, to, move.promoPiece(), captured);
        else if (move.isEnPassant())  makeEnPassantMove(from, to);
        else                          makeStandardMove(from, to, piece, captured);

        updateState(from, to, piece, captured, move);
        moves[ply++] = move;
        white = !white;
        return true;

    }

    /**
     * Reverts the internal board representation to the state before the last move. Toggles the piece bitboards to move the
     * piece + reinstate the captured piece, plus special rules for pawn double-moves, castling, promotion and en passant.
     */
    public void unmakeMove() {

        white = !white;
        final Move move = moves[--ply];
        final int from = move.from();
        final int to = move.to();
        final Piece piece = pieceAt(to);

        if (move.isCastling())        unmakeCastlingMove(from, to);
        else if (move.isPromotion())  unmakePromotionMove(from, to, move.promoPiece());
        else if (move.isEnPassant())  unmakeEnPassantMove(from, to);
        else                          unmakeStandardMove(from, to, piece);

        state = states[ply];

    }

    private void makePawnDoubleMove(int from, int to) {
        movePiece(Piece.PAWN, white, from, to, true);
    }

    private void makeCastleMove(int from, int to) {
        movePiece(Piece.KING, white, from, to, true);
        final boolean kingside = File.of(to) == 6;
        final int rookFrom = Castling.rookFrom(kingside, white);
        final int rookTo = Castling.rookTo(kingside, white);
        movePiece(Piece.ROOK, white, rookFrom, rookTo, true);
    }

    private void makeEnPassantMove(int from, int to) {
        final int pawnSquare = white ? to - 8 : to + 8;
        unsetPiece(Piece.PAWN, !white, pawnSquare, true);
        movePiece(Piece.PAWN, white, from, to, true);
    }

    private void makePromotionMove(int from, int to, Piece promoted, Piece captured) {
        unsetPiece(Piece.PAWN, white, from, true);
        if (captured != null) {
            unsetPiece(captured, !white, to, true);
        }
        setPiece(promoted, white, to, true);
    }

    private void makeStandardMove(int from, int to, Piece piece, Piece captured) {
        if (captured != null) {
            unsetPiece(captured, !white, to, true);
        }
        movePiece(piece, white, from, to, true);
    }

    private void updateState(int from, int to, Piece piece, Piece captured, Move move) {
        state.captured = captured;
        final boolean resetClock = captured != null || Piece.PAWN.equals(piece);
        state.halfMoveClock = resetClock ? 0 : ++state.halfMoveClock;

        final int castleRights = updateCastleRights(from, to, piece);
        state.key = Zobrist.updateCastlingRights(state.key, state.rights, castleRights);
        state.rights = castleRights;

        final int enPassantFile = move.isPawnDoubleMove() ? File.of(to) : -1;
        state.key = Zobrist.updateEnPassantFile(state.key, state.enPassantFile, enPassantFile);
        state.enPassantFile = enPassantFile;

        state.key = Zobrist.updateSideToMove(state.key);
    }

    private void unmakeCastlingMove(int from, int to) {
        movePiece(Piece.KING, white, to, from, false);
        final boolean kingside = File.of(to) == 6;
        final int rookFrom = Castling.rookTo(kingside, white);
        final int rookTo = Castling.rookFrom(kingside, white);
        movePiece(Piece.ROOK, white, rookFrom, rookTo, false);
    }

    private void unmakePromotionMove(int from, int to, Piece promotionPiece) {
        unsetPiece(promotionPiece, white, to, false);
        if (state.getCaptured() != null) {
            setPiece(state.getCaptured(), !white, to, false);
        }
        setPiece(Piece.PAWN, white, from, false);
    }

    private void unmakeEnPassantMove(int from, int to) {
        movePiece(Piece.PAWN, white, to, from, false);
        final int captureSquare = white ? to - 8 : to + 8;
        setPiece(Piece.PAWN, !white, captureSquare, false);
    }

    private void unmakeStandardMove(int from, int to, Piece piece) {
        Piece captured = state.getCaptured();
        if (captured != null) {
            unsetPiece(piece, white, to, false);
            setPiece(captured, !white, to, false);
            setPiece(piece, white, from, false);
        } else {
            movePiece(piece, white, to, from, false);
        }
    }

    /**
     * Make a 'null' move, meaning the side to move passes their turn and gives the opponent a double-move. Used exclusively
     * during null-move pruning during search.
     */
    public void makeNullMove() {
        white = !white;
        final long key = Zobrist.updateKeyAfterNullMove(state.key, state.enPassantFile);
        final long[] nonPawnKeys = new long[] {state.nonPawnKeys[0], state.nonPawnKeys[1]};
        final GameState newState = new GameState(key, state.pawnKey, nonPawnKeys, null, -1, state.getRights(), 0);
        states[ply++] = state;
        state = newState;
    }

    /**
     * Unmake the 'null' move used during null-move pruning to try and prove a beta cut-off.
     */
    public void unmakeNullMove() {
        white = !white;
        state = states[--ply];
    }

    public void movePiece(Piece type, boolean white, int from, int to, boolean updateKeys) {
        final long toggleMask = Bits.of(from) | Bits.of(to);
        toggle(type, white, toggleMask);
        pieces[from] = null;
        pieces[to] = type;
        if (updateKeys) {
            state.key = Zobrist.updatePiece(state.key, from, to, type, white);
            if (type == Piece.PAWN) {
                state.pawnKey = Zobrist.updatePiece(state.pawnKey, from, to, type, white);
            } else {
                int colourIndex = Colour.index(white);
                state.nonPawnKeys[colourIndex] = Zobrist.updatePiece(state.nonPawnKeys[colourIndex], from, to, type, white);
            }
        }
    }

    public void setPiece(Piece type, boolean white, int square, boolean updateKeys) {
        final long toggleMask = Bits.of(square);
        toggle(type, white, toggleMask);
        pieces[square] = type;
        if (updateKeys) {
            state.key = Zobrist.updatePiece(state.key, square, type, white);
            if (type == Piece.PAWN) {
                state.pawnKey = Zobrist.updatePiece(state.pawnKey, square, type, white);
            } else {
                int colourIndex = Colour.index(white);
                state.nonPawnKeys[colourIndex] = Zobrist.updatePiece(state.nonPawnKeys[colourIndex], square, type, white);
            }
        }
    }

    public void unsetPiece(Piece type, boolean white, int square, boolean updateKeys) {
        final long toggleMask = Bits.of(square);
        toggle(type, white, toggleMask);
        pieces[square] = null;
        if (updateKeys) {
            state.key = Zobrist.updatePiece(state.key, square, type, white);
            if (type == Piece.PAWN) {
                state.pawnKey = Zobrist.updatePiece(state.pawnKey, square, type, white);
            } else {
                int colourIndex = Colour.index(white);
                state.nonPawnKeys[colourIndex] = Zobrist.updatePiece(state.nonPawnKeys[colourIndex], square, type, white);
            }
        }
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
        final long toggleMask = white ? (kings & whitePieces) : (kings & blackPieces);
        kings ^= toggleMask;
        if (white) {
            whitePieces ^= toggleMask;
        } else {
            blackPieces ^= toggleMask;
        }
        occupied ^= toggleMask;
    }

    public void addKing(int kingSquare, boolean white) {
        final long toggleMask = Bits.of(kingSquare);
        kings |= toggleMask;
        if (white) {
            whitePieces |= toggleMask;
        } else {
            blackPieces |= toggleMask;
        }
        occupied |= toggleMask;
    }

    private int updateCastleRights(int from, int to, Piece pieceType) {
        int newRights = state.getRights();
        if (newRights == 0b0000) {
            // Both sides already lost castling rights, so nothing to calculate.
            return newRights;
        }
        // Any move by the king removes castling rights.
        if (Piece.KING.equals(pieceType)) {
            newRights &= white ? Castling.CLEAR_WHITE_CASTLING_MASK : Castling.CLEAR_BLACK_CASTLING_MASK;
        }
        // Any move starting from/ending at a rook square removes castling rights for that corner.
        // Note: all of these cases need to be checked, to cover the scenario where a rook in starting position captures
        // another rook in starting position; in that case, both sides lose castling rights!
        if (from == 7 || to == 7) {
            newRights &= Castling.CLEAR_WHITE_KINGSIDE_MASK;
        }
        if (from == 63 || to == 63) {
            newRights &= Castling.CLEAR_BLACK_KINGSIDE_MASK;
        }
        if (from == 0 || to == 0) {
            newRights &= Castling.CLEAR_WHITE_QUEENSIDE_MASK;
        }
        if (from == 56 || to == 56) {
            newRights &= Castling.CLEAR_BLACK_QUEENSIDE_MASK;
        }
        return newRights;
    }

    public Piece pieceAt(int square) {
        return pieces[square];
    }

    public boolean isCapture(Move move) {
        return move.isEnPassant() || pieceAt(move.to()) != null;
    }

    public boolean isQuiet(Move move) {
        return !move.isPromotion() && !isCapture(move);
    }

    public long getPawns(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return pawns & side;
    }

    public long getKnights(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return knights & side;
    }

    public long getBishops(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return bishops & side;
    }

    public long getRooks(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return rooks & side;
    }

    public long getQueens(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return queens & side;
    }

    public long getKing(boolean white) {
        final long side = white ? whitePieces : blackPieces;
        return kings & side;
    }

    public long getPieces(boolean white) {
        return white ? whitePieces : blackPieces;
    }

    public void setPawns(long pawns) {
        this.pawns = pawns;
    }

    public void setKnights(long knights) {
        this.knights = knights;
    }

    public void setBishops(long bishops) {
        this.bishops = bishops;
    }

    public void setRooks(long rooks) {
        this.rooks = rooks;
    }

    public void setQueens(long queens) {
        this.queens = queens;
    }

    public void setKings(long kings) {
        this.kings = kings;
    }

    public void setWhitePieces(long whitePieces) {
        this.whitePieces = whitePieces;
    }

    public void setBlackPieces(long blackPieces) {
        this.blackPieces = blackPieces;
    }

    public void setOccupied(long occupied) {
        this.occupied = occupied;
    }

    public void setPieces(Piece[] pieces) {
        this.pieces = pieces;
    }

    public void setWhite(boolean white) {
        this.white = white;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public void setStates(GameState[] states) {
        this.states = states;
    }

    public void setMoves(Move[] moves) {
        this.moves = moves;
    }

    public long getPawns() {
        return pawns;
    }

    public long getKnights() {
        return knights;
    }

    public long getBishops() {
        return bishops;
    }

    public long getRooks() {
        return rooks;
    }

    public long getQueens() {
        return queens;
    }

    public long getKings() {
        return kings;
    }

    public long getWhitePieces() {
        return whitePieces;
    }

    public long getBlackPieces() {
        return blackPieces;
    }

    public long getOccupied() {
        return occupied;
    }

    public Piece[] getPieces() {
        return pieces;
    }

    public boolean isWhite() {
        return white;
    }

    public GameState getState() {
        return state;
    }

    public GameState[] getStates() {
        return states;
    }

    public Move[] getMoves() {
        return moves;
    }

    public int getPly() {
        return ply;
    }

    public long key() {
        return state.getKey();
    }

    public long pawnKey() {
        return state.getPawnKey();
    }

    public int countPieces() {
        return Bits.count(occupied);
    }

    public boolean hasPiecesRemaining(boolean white) {
        return white ?
                (getKnights(true) != 0 || getBishops(true) != 0 || getRooks(true) != 0 || getQueens(true) != 0) :
                (getKnights(false) != 0 || getBishops(false) != 0 || getRooks(false) != 0 || getQueens(false) != 0);
    }

    public static Board from(String fen) {
        return FEN.toBoard(fen);
    }

    public Board copy() {
        final Board newBoard = new Board();
        newBoard.setPawns(this.getPawns());
        newBoard.setKnights(this.getKnights());
        newBoard.setBishops(this.getBishops());
        newBoard.setRooks(this.getRooks());
        newBoard.setQueens(this.getQueens());
        newBoard.setKings(this.getKings());
        newBoard.setWhitePieces(this.getWhitePieces());
        newBoard.setBlackPieces(this.getBlackPieces());
        newBoard.setOccupied(this.getOccupied());
        newBoard.setWhite(this.isWhite());
        newBoard.setState(this.getState().copy());
        GameState[] newStates = new GameState[this.getStates().length];
        for (int i = 0; i < this.getStates().length; i++) {
            if (this.getStates()[i] == null) {
                break;
            }
            newStates[i] = this.getStates()[i].copy();
        }
        newBoard.setStates(newStates);
        Move[] newMoves = new Move[this.getMoves().length];
        for (int i = 0; i < this.getMoves().length; i++) {
            if (this.getMoves()[i] == null) {
                break;
            }
            newMoves[i] = new Move(this.getMoves()[i].value());
        }
        newBoard.setMoves(newMoves);
        newBoard.setPieces(Arrays.copyOf(this.getPieces(), this.getPieces().length));
        return newBoard;
    }

    public void print() {

        for (int rank = 7; rank >= 0; --rank) {
            System.out.print(" +---+---+---+---+---+---+---+---+\n");

            for (int file = 0; file < 8; ++file) {
                int sq = Square.of(rank, file);
                Piece piece = pieceAt(sq);
                if (piece == null) {
                    System.out.print(" |  ");
                    continue;
                }
                boolean white = (whitePieces & Bits.of(sq)) != 0;
                System.out.print(" | " + (white ? piece.code().toUpperCase() : piece.code()));
            }

            System.out.print(" | " + (rank + 1) + "\n");
        }

        System.out.print(" +---+---+---+---+---+---+---+---+\n");
        System.out.print("   a   b   c   d   e   f   g   h\n\n");

        System.out.print((white ? "White" : "Black") + " to move\n");

    }

}
