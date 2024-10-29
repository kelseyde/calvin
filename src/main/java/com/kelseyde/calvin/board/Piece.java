package com.kelseyde.calvin.board;

import static com.kelseyde.calvin.board.Move.*;

/**
 * Stores basic information for each chess piece type.
 */
public enum Piece {

    PAWN    (0, 100, "p"),
    KNIGHT  (1, 320, "n"),
    BISHOP  (2, 330, "b"),
    ROOK    (3, 500, "r"),
    QUEEN   (4, 900, "q"),
    KING    (5, 0, "k");

    public static final int COUNT = 6;

    final int index;

    final int value;

    final String code;

    Piece(int index, int value, String code) {
        this.index = index;
        this.value = value;
        this.code = code;
    }

    public int index() {
        return index;
    }

    public int value() {
        return value;
    }

    public String code() {
        return code;
    }

    public boolean isSlider() {
        return this == BISHOP || this == ROOK || this == QUEEN;
    }

    public static short promoFlag(Piece piece) {
        if (piece == null) {
            return MoveFlag.NONE;
        }
        return switch (piece) {
            case QUEEN -> MoveFlag.PROMO_QUEEN;
            case ROOK -> MoveFlag.PROMO_ROOK;
            case BISHOP -> MoveFlag.PROMO_BISHOP;
            case KNIGHT -> MoveFlag.PROMO_KNIGHT;
            default -> MoveFlag.NONE;
        };
    }

}
