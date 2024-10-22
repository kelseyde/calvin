package com.kelseyde.calvin.board;


import com.kelseyde.calvin.board.Bits.Castling;

/**
 * Stores the metadata for a given chess position - that is, the castling rights, en passant rights, the fifty-move counter
 * (the number of half-moves since the last capture or pawn move), and the last captured piece.
 * The game state history is stored by the {@link Board} to easily 'unmake' moves during search + evaluation.
 */
public class BoardState {

    public long key;
    public long pawnKey;
    public long[] nonPawnKeys;
    public int enPassantFile;
    public int rights;
    public int halfMoveClock;
    public Piece captured;
    public long checkers;
    public long pinned;
    public long threats;

    public BoardState() {
        this.key = 0L;
        this.pawnKey = 0L;
        this.nonPawnKeys = new long[2];
        this.captured = null;
        this.enPassantFile = -1;
        this.rights = Castling.INITIAL_CASTLING_RIGHTS;
        this.halfMoveClock = 0;
        this.checkers = 0L;
        this.pinned = 0L;
        this.threats = 0L;
    }

    public BoardState(long key, long pawnKey, long[] nonPawnKeys, Piece captured, int enPassantFile, int rights, int halfMoveClock,
            long checkers, long pinned, long threats) {
        this.key = key;
        this.pawnKey = pawnKey;
        this.nonPawnKeys = nonPawnKeys;
        this.captured = captured;
        this.enPassantFile = enPassantFile;
        this.rights = rights;
        this.halfMoveClock = halfMoveClock;
        this.checkers = checkers;
        this.pinned = pinned;
        this.threats = threats;
    }

    public boolean isKingsideCastlingAllowed(boolean white) {
        long kingsideMask = white ? 0b0001 : 0b0100;
        return (rights & kingsideMask) == kingsideMask;
    }

    public boolean isQueensideCastlingAllowed(boolean white) {
        long queensideMask = white ? 0b0010 : 0b1000;
        return (rights & queensideMask) == queensideMask;
    }

    public long getKey() {
        return key;
    }

    public long getPawnKey() {
        return pawnKey;
    }

    public Piece getCaptured() {
        return captured;
    }

    public int getEnPassantFile() {
        return enPassantFile;
    }

    public int getRights() {
        return rights;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public void setPawnKey(long pawnKey) {
        this.pawnKey = pawnKey;
    }

    public void setNonPawnKeys(long[] nonPawnKeys) {
        this.nonPawnKeys = nonPawnKeys;
    }

    public void setEnPassantFile(int enPassantFile) {
        this.enPassantFile = enPassantFile;
    }

    public void setRights(int rights) {
        this.rights = rights;
    }

    public void setHalfMoveClock(int halfMoveClock) {
        this.halfMoveClock = halfMoveClock;
    }

    public void setCheckers(long checkers) {
        this.checkers = checkers;
    }

    public void setPinned(long pinned) {
        this.pinned = pinned;
    }

    public void setThreats(long threats) {
        this.threats = threats;
    }

    public BoardState copy() {
        long[] nonPawnKeysCopy = new long[]{nonPawnKeys[0], nonPawnKeys[1]};
        return new BoardState(
                key, pawnKey, nonPawnKeysCopy, captured, enPassantFile, rights, halfMoveClock, checkers, pinned, threats);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardState boardState = (BoardState) o;
        return key == boardState.key
                && pawnKey == boardState.pawnKey
                && enPassantFile == boardState.enPassantFile
                && rights == boardState.rights
                && halfMoveClock == boardState.halfMoveClock
                && captured == boardState.captured
                && checkers == boardState.checkers
                && pinned == boardState.pinned
                && threats == boardState.threats;
    }

}
