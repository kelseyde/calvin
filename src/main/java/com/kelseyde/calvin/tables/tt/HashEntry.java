package com.kelseyde.calvin.tables.tt;

import com.kelseyde.calvin.board.Move;

/**
 * Entry in the {@link TranspositionTable}.
 * </p>
 * Records the move, score, static evaluation, flag, and depth of a position that has been searched. When stored in the
 * table, this information is packed into two 64-bit longs: a key and a value. The encoding scheme is as follows:
 * - Key: 0-31 (zobrist key), 32-47 (age), 48-63 (static eval)
 * - Value: 0-11 (depth), 12-15 (flag), 16-31 (move), 32-63 (score)
 */
public record HashEntry(Move move, int score, int staticEval, HashFlag flag, int depth) {

    public static HashEntry of(long key, int value, int score) {
        final Move move       = Value.getMove(value);
        final HashFlag flag   = Value.getFlag(value);
        final int depth       = Value.getDepth(value);
        final int staticEval  = Key.getStaticEval(key);
        return new HashEntry(move, score, staticEval, flag, depth);
    }

    public static class Key {

        private static final long STATIC_EVAL_MASK    = 0xffff000000000000L;
        private static final long AGE_MASK            = 0x0000ffff00000000L;
        private static final long ZOBRIST_PART_MASK   = 0x00000000ffffffffL;

        public static long getZobristPart(long key) {
            return key & ZOBRIST_PART_MASK;
        }

        public static int getAge(long key) {
            return (int) ((key & AGE_MASK) >>> 32);
        }

        public static long setAge(long key, int age) {
            return (key & ~AGE_MASK) | ((long) age << 32);
        }

        public static int getStaticEval(long key) {
            return (short) ((key & STATIC_EVAL_MASK) >>> 48);
        }

        public static long of(long zobristKey, int staticEval, int age) {
            return (zobristKey & ZOBRIST_PART_MASK) | ((long) age << 32) | ((long) (staticEval & 0xFFFF) << 48);
        }

    }

    public static class Value {

        private static final long MOVE_MASK     = 0xffff0000L;
        private static final long FLAG_MASK     = 0x0000f000L;
        private static final long DEPTH_MASK    = 0x00000fffL;

        public static Move getMove(int value) {
            int move = (int) ((value & MOVE_MASK) >>> 16);
            return move > 0 ? new Move((short) move) : null;
        }

        public static int setMove(int value, Move move) {
            return (int) ((value &~ MOVE_MASK) | (long) move.value() << 16);
        }

        public static HashFlag getFlag(int value) {
            long flag = (value & FLAG_MASK) >>> 12;
            return HashFlag.valueOf((int) flag);
        }

        public static int getDepth(long value) {
            return (int) (value & DEPTH_MASK);
        }

        public static int of(Move move, HashFlag flag, int depth) {
            long moveValue = move != null ? move.value() : 0;
            long flagValue = HashFlag.value(flag);
            return (int) (moveValue << 16 | flagValue << 12 | depth);
        }

    }

}