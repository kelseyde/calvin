package com.kelseyde.calvin.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores the metadata for a given chess position - that is, the castling rights, en passant rights, the fifty-move counter
 * (the number of half-moves since the last capture or pawn move), and the last captured piece.
 * The game state history is stored by the {@link Board} to easily 'unmake' moves during search + evaluation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameState {

    long key = 0L;
    long pawnKey = 0L;
    long[] nonPawnKey = new long[2];
    Piece capturedPiece;
    int enPassantFile = -1;
    int castlingRights = Bits.INITIAL_CASTLING_RIGHTS;
    int halfMoveClock = 0;

    public boolean isKingsideCastlingAllowed(boolean white) {
        long kingsideMask = white ? 0b0001 : 0b0100;
        return (castlingRights & kingsideMask) == kingsideMask;
    }

    public boolean isQueensideCastlingAllowed(boolean white) {
        long queensideMask = white ? 0b0010 : 0b1000;
        return (castlingRights & queensideMask) == queensideMask;
    }

    public GameState copy() {
        return new GameState(key, pawnKey, nonPawnKey, capturedPiece, enPassantFile, castlingRights, halfMoveClock);
    }

}
