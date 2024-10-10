package com.kelseyde.calvin.tables.tt;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.search.Score;

import java.util.stream.IntStream;

/**
 * The transposition table is a database that stores the results of previously searched positions, as well as relevant
 * information about that position, such as the depth to which it was searched, and the best move found during the previous
 * search.
 * </p>
 * Since many positions can be arrived at by several different move orders, a simple brute-force search of the search tree
 * encounters the same positions again and again (via 'transposition'). A transposition table, therefore, greatly reduces
 * the size of the search tree, since subsequent arrivals at the position can re-use the results of previous searches.
 * </p>
 * @see <a href="https://www.chessprogramming.org/Transposition_Table">Chess Programming Wiki</a>
 */
public class TranspositionTable {

    private static final int BUCKET_SIZE = 4;
    private static final int ENTRY_SIZE_BYTES = 16;

    private long[] keys;
    private long[] values;
    private int size;
    private int age;
    private int tries;
    private int hits;

    /**
     * Constructs a transposition table of the given size in megabytes.
     */
    public TranspositionTable(int tableSizeMb) {
        this.size = (tableSizeMb * 1024 * 1024) / ENTRY_SIZE_BYTES;
        this.keys = new long[size];
        this.values = new long[size];
        this.tries = 0;
        this.hits = 0;
        this.age = 0;
    }

    /**
     * Retrieves an entry from the transposition table using the given zobrist key.
     *
     * @param key the zobrist key of the position.
     * @param ply the current ply in the search (used to adjust mate scores).
     */
    public HashEntry get(long key, int ply) {
        int index = index(key);
        tries++;
        for (int i = 0; i < BUCKET_SIZE; i++) {
            long storedKey = keys[index + i];
            if (storedKey != 0 && HashEntry.Key.getZobristPart(storedKey) == HashEntry.Key.getZobristPart(key)) {
                hits++;
                storedKey = HashEntry.Key.setAge(storedKey, age);
                keys[index + i] = storedKey;
                long storedValue = values[index + i];
                int score = HashEntry.Value.getScore(storedValue);
                if (Score.isMateScore(score)) {
                    score = retrieveMateScore(score, ply);
                    storedValue = HashEntry.Value.setScore(storedValue, score);
                }
                return HashEntry.of(storedKey, storedValue);
            }
        }
        return null;
    }

    /**
     * Puts an entry into the transposition table.
     * </p>
     * The transposition table is separated into buckets of 4 entries each. This method uses a replacement scheme that
     * prefers to replace the least-valuable entry among the 4 candidates in the bucket. The order of preference
     * for replacement is:
     * <ol>
     * <li>An empty entry.</li>
     * <li>An entry with the same zobrist key and a depth less than or equal to the new entry.</li>
     * <li>The oldest entry in the bucket, stored further back in the game and so less likely to be relevant.</li>
     * <li>The entry with the lowest depth.</li>
     * </ol>
     *
     * @param key the zobrist key of the position.
     * @param flag the flag indicating the type of node (e.g., exact, upper bound, lower bound).
     * @param depth the search depth of the entry.
     * @param ply the current ply from root in the search.
     * @param move the best move found at this position.
     * @param score the score of the position.
     */
    public void put(long key, HashFlag flag, int depth, int ply, Move move, int staticEval, int score) {

        // Get the start index of the 4-item bucket.
        final int startIndex = index(key);

        // If the eval is checkmate, adjust the score to reflect the number of ply from the root position
        if (Score.isMateScore(score)) score = calculateMateScore(score, ply);

        int replacedIndex = -1;
        int minDepth = Integer.MAX_VALUE;
        boolean replacedByAge = false;

        // Iterate over the four items in the bucket
        for (int i = startIndex; i < startIndex + 4; i++) {
            long storedKey = keys[i];

            // First, always prefer an empty slot if it is available.
            if (storedKey == 0) {
                replacedIndex = i;
                break;
            }

            long storedValue = values[i];
            int storedDepth = HashEntry.Value.getDepth(values[i]);
            // Then, if the stored entry matches the zobrist key and the depth is >= the stored depth, replace it.
            // If the depth is < the store depth, don't replace it and exit (although this should never happen).
            if (HashEntry.Key.getZobristPart(storedKey) == HashEntry.Key.getZobristPart(key)) {
                if (depth >= storedDepth) {
                    // If the stored entry has a recorded best move but the new entry does not, use the stored one.
                    Move storedMove = HashEntry.Value.getMove(storedValue);
                    if (move == null && storedMove != null) {
                        move = storedMove;
                    }
                    replacedIndex = i;
                    break;
                } else {
                    return;
                }
            }

            // Next, prefer to replace entries from earlier on in the game, since they are now less likely to be relevant.
            if (age > HashEntry.Key.getAge(storedKey)) {
                replacedByAge = true;
                replacedIndex = i;
            }

            // Finally, just replace the entry with the shallowest search depth.
            if (!replacedByAge && storedDepth < minDepth) {
                minDepth = storedDepth;
                replacedIndex = i;
            }

        }

        // Store the new entry in the table at the chosen index.
        if (replacedIndex != -1) {
            keys[replacedIndex] = HashEntry.Key.of(key, staticEval, age);
            values[replacedIndex] = HashEntry.Value.of(score, move, flag, depth);
        }
    }

    /**
     * Calculate how full the transposition table currently is.
     * @return the number of entries out of 1000 that are currently not-null.
     */
    public int fill() {
        return (int) IntStream.range(0, 1000)
                .filter(i -> keys[i] != 0)
                .count();
    }

    /**
     * Increments the age counter for the transposition table.
     */
    public void incrementAge() {
        this.age++;
    }

    public void resize(int tableSizeMb) {
        this.size = (tableSizeMb * 1024 * 1024) / ENTRY_SIZE_BYTES;
        this.keys = new long[size];
        this.values = new long[size];
        this.tries = 0;
        this.hits = 0;
        this.age = 0;
    }

    /**
     * Clears the transposition table, resetting all entries and statistics.
     */
    public void clear() {
        this.keys = new long[size];
        this.values = new long[size];
        this.tries = 0;
        this.hits = 0;
        this.age = 0;
    }

    /**
     * Compresses the 64-bit zobrist key into a 32-bit key, to be used as an index in the hash table.
     */
    private int index(long key) {
        // XOR the upper and lower halves of the zobrist key together, producing a pseudo-random 32-bit result.
        // Then apply a mask ensuring the number is always positive, since it is to be used as an array index.
        long index = (key ^ (key >>> 32)) & 0x7FFFFFFF;
        // Modulo the result with the number of entries in the table, and align it with a multiple of 4,
        // ensuring the entries are always divided into 4-sized buckets.
        return (int) (index % (size - (BUCKET_SIZE - 1))) & -BUCKET_SIZE;
    }

    // On insertion, adjust the mate score to reflect the number of ply from the root position
    private int calculateMateScore(int score, int plyFromRoot) {
        return score > 0 ? score - plyFromRoot : score + plyFromRoot;
    }

    private int retrieveMateScore(int score, int plyFromRoot) {
        // On retrieval, adjust the mate score to reflect the number of ply from the root position
        return score > 0 ? score + plyFromRoot : score - plyFromRoot;
    }

}