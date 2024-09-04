package com.kelseyde.calvin.tables.tt;

import com.kelseyde.calvin.board.Move;
import lombok.AllArgsConstructor;

/**
 * Entry in the {@link TranspositionTable}. Contains a 64-bit key and a 64-bit value which encodes the relevant
 * information about the position.
 * </p>
 *
 * Key encoding:
 * 0-31: 32 bits representing half of the zobrist hash. Used to verify that the position truly matches.
 * 32-47: 16 bits representing the generation of the entry, i.e. how old it is. Used to gradually replace old entries.
 * 48-63: 16 bits representing the static eval of the position. Re-used to save calling the evaluation function again.
 * </p>
 *
 * Value encoding:
 * 0-11: the depth to which this position was last searched.
 * 12-15: the {@link HashFlag} indicating what type of node this is.
 * 16-31: the {@link Move} start square, end square, and special move flag.
 * 32-63: the eval of the position in centipawns.
 */
@AllArgsConstructor
public class HashEntry {

    public static final int SIZE_BYTES = 32;

    private static final long ZOBRIST_PART_MASK = 0x00000000ffffffffL;
    private static final long GENERATION_MASK = 0x0000ffff00000000L;
    private static final long STATIC_EVAL_MASK = 0xffff000000000000L;
    private static final long SCORE_MASK = 0xffffffff00000000L;
    private static final long MOVE_MASK = 0x00000000ffff0000L;
    private static final long FLAG_MASK = 0x000000000000f000L;
    private static final long DEPTH_MASK = 0x0000000000000fffL;

    private long key;
    private long value;

    /**
     * Extracts the 48-bits representing the zobrist part of the given zobrist key.
     */
    public static long zobristPart(long zobrist) {
        return zobrist & ZOBRIST_PART_MASK;
    }

    /**
     * Returns the 48-bits representing zobrist part of the hash entry key.
     */
    public long getZobristPart() {
        return key & ZOBRIST_PART_MASK;
    }

    /**
     * Gets the generation part of this entry's key.
     */
    public int getGeneration() {
        return (int) ((key & GENERATION_MASK) >>> 32);
    }

    /**
     * Sets the generation part of this entry's key.
     */
    public void setGeneration(int generation) {
        key = (key & ~GENERATION_MASK) | ((long) generation << 32);
    }

    /**
     * Gets the static eval part of this entry's key.
     */
    public int getStaticEval() {
        return (short) ((key & STATIC_EVAL_MASK) >>> 48);
    }

    /**
     * Sets the static eval part of this entry's key.
     */
    public void setStaticEval(int staticEval) {
        key = (key & ~STATIC_EVAL_MASK) | ((long) (staticEval & 0xFFFF) << 48);
    }

    /**
     * Gets the score from this entry's value.
     */
    public int getScore() {
        long score = (value & SCORE_MASK) >>> 32;
        return (int) score;
    }

    /**
     * Sets the score in this entry's value.
     */
    public void setScore(int score) {
        value = (value &~ SCORE_MASK) | (long) score << 32;
    }

    /**
     * Creates a new {@link HashEntry} with the adjusted score.
     */
    public HashEntry withAdjustedScore(int score) {
        long newValue = (value &~ SCORE_MASK) | (long) score << 32;
        return new HashEntry(key, newValue);
    }

    /**
     * Sets the move in this entry's value.
     */
    public void setMove(Move move) {
        value = (value &~ MOVE_MASK) | (long) move.value() << 16;
    }

    /**
     * Gets the move from this entry's value.
     */
    public Move getMove() {
        long move = (value & MOVE_MASK) >>> 16;
        return move > 0 ? new Move((short) move) : null;
    }

    /**
     * Gets the flag from this entry's value.
     */
    public HashFlag getFlag() {
        long flag = (value & FLAG_MASK) >>> 12;
        return HashFlag.valueOf((int) flag);
    }

    /**
     * Gets the depth from this entry's value.
     */
    public int getDepth() {
        return (int) (value & DEPTH_MASK);
    }

    /**
     * Creates a new {@link HashEntry} with the specified parameters.
     *
     * @param zobristKey the Zobrist key
     * @param score the score
     * @param move the move
     * @param flag the flag
     * @param depth the depth
     * @param generation the generation
     * @return a new {@link HashEntry}
     */
    public static HashEntry of(long zobristKey, int score, int staticEval, Move move, HashFlag flag, int depth, int generation) {
        // Build the key using 32 bits for the zobrist part, 16 bits for the generation part, and 16 bits for the static evaluation part.
        long key = (zobristKey & ZOBRIST_PART_MASK) | ((long) generation << 32) | ((long) (staticEval & 0xFFFF) << 48);
        // Get the 16-bit encoded move
        long moveValue = move != null ? move.value() : 0;
        // Get the 3-bit encoded flag
        long flagValue = HashFlag.value(flag);
        // Combine the score, move, flag and depth to create the hash entry value
        long value = (long) score << 32 | moveValue << 16 | flagValue << 12 | depth;
        return new HashEntry(key, value);
    }

}