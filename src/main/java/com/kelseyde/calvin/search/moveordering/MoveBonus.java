package com.kelseyde.calvin.search.moveordering;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class MoveBonus {

    public static final int MILLION = 1000000;

    public static final int TT_MOVE_BIAS = 10 * MILLION;
    public static final int QUEEN_PROMOTION_BIAS = 9 * MILLION;
    public static final int WINNING_CAPTURE_BIAS = 8 * MILLION;
    public static final int EQUAL_CAPTURE_BIAS = 7 * MILLION;
    public static final int KILLER_MOVE_BIAS = 6 * MILLION;
    public static final int LOSING_CAPTURE_BIAS = 5 * MILLION;
    public static final int HISTORY_MOVE_BIAS = 4 * MILLION;
    public static final int UNDER_PROMOTION_BIAS = 3 * MILLION;
    public static final int CASTLING_BIAS = 2 * MILLION;

}
