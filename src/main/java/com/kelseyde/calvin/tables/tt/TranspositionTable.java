package com.kelseyde.calvin.tables.tt;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.evaluation.Score;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranspositionTable {

    final int tableSize;
    HashEntry[] entries;

    int tries;
    int hits;
    int generation;

    /**
     * Constructs a transposition table of the given size in megabytes.
     */
    public TranspositionTable(int tableSizeMb) {
        this.tableSize = (tableSizeMb * 1024 * 1024) / HashEntry.SIZE_BYTES;
        entries = new HashEntry[tableSize];
        tries = 0;
        hits = 0;
        generation = 0;
    }

    /**
     * Retrieves an entry from the transposition table using the given zobrist key.
     *
     * @param zobristKey the zobrist key of the position.
     * @param ply the current ply in the search (used to adjust mate scores).
     */
    public HashEntry get(long zobristKey, int ply) {
        int index = getIndex(zobristKey);
        long zobristPart = HashEntry.zobristPart(zobristKey);
        tries++;
        for (int i = 0; i < 4; i++) {
            HashEntry entry = entries[index + i];
            if (entry != null && entry.getZobristPart() == zobristPart) {
                hits++;
                entry.setGeneration(generation);
                if (Score.isMateScore(entry.getScore())) {
                    int score = retrieveMateScore(entry.getScore(), ply);
                    return entry.withAdjustedScore(score);
                }
                return entry;
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
     * @param zobristKey the zobrist key of the position.
     * @param flag the flag indicating the type of node (e.g., exact, upper bound, lower bound).
     * @param depth the search depth of the entry.
     * @param ply the current ply from root in the search.
     * @param move the best move found at this position.
     * @param score the score of the position.
     */
    public void put(long zobristKey, HashFlag flag, int depth, int ply, Move move, int staticEval, int score) {

        // Get the start index of the 4-item bucket.
        int startIndex = getIndex(zobristKey);

        // If the eval is checkmate, adjust the score to reflect the number of ply from the root position
        if (Score.isMateScore(score)) score = calculateMateScore(score, ply);

        int replacedIndex = -1;
        int minDepth = Integer.MAX_VALUE;
        boolean replacedByAge = false;

        // Iterate over the four items in the bucket
        for (int i = startIndex; i < startIndex + 4; i++) {
            HashEntry storedEntry = entries[i];

            // First, always prefer an empty slot if it is available.
            if (storedEntry == null) {
                replacedIndex = i;
                break;
            }

            // Then, if the stored entry matches the zobrist key and the depth is >= the stored depth, replace it.
            // If the depth is < the store depth, don't replace it and exit (although this should never happen).
            if (storedEntry.getZobristPart() == HashEntry.zobristPart(zobristKey)) {
                if (depth >= storedEntry.getDepth()) {
                    // If the stored entry has a recorded best move but the new entry does not, use the stored one.
                    if (move == null && storedEntry.getMove() != null) {
                        move = storedEntry.getMove();
                    }
                    replacedIndex = i;
                    break;
                } else {
                    return;
                }
            }

            // Next, prefer to replace entries from earlier on in the game, since they are now less likely to be relevant.
            if (generation > storedEntry.getGeneration()) {
                replacedByAge = true;
                replacedIndex = i;
            }

            // Finally, just replace the entry with the shallowest search depth.
            if (!replacedByAge && storedEntry.getDepth() < minDepth) {
                minDepth = storedEntry.getDepth();
                replacedIndex = i;
            }

        }

        // Store the new entry in the table at the chosen index.
        if (replacedIndex != -1) {
            entries[replacedIndex] = HashEntry.of(zobristKey, score, staticEval, move, flag, depth, generation);;
        }
    }

    /**
     * Increments the generation counter for the transposition table.
     */
    public void incrementGeneration() {
        generation++;
    }

    /**
     * Clears the transposition table, resetting all entries and statistics.
     */
    public void clear() {
        tries = 0;
        hits = 0;
        generation = 0;
        entries = new HashEntry[tableSize];
    }

    /**
     * Compresses the 64-bit zobrist key into a 32-bit key, to be used as an index in the hash table.
     *
     * @param zobristKey the zobrist key of the position.
     * @return a compressed 32-bit index.
     */
    private int getIndex(long zobristKey) {
        // XOR the upper and lower halves of the zobrist key together, producing a pseudo-random 32-bit result.
        // Then apply a mask ensuring the number is always positive, since it is to be used as an array index.
        long index = (zobristKey ^ (zobristKey >>> 32)) & 0x7FFFFFFF;
        // Modulo the result with the number of entries in the table, and align it with a multiple of 4,
        // ensuring the entries are always divided into 4-sized buckets.
        return (int) (index % (tableSize - 3)) & ~3;
    }

    /**
     * Calculates the mate score, adjusting it based on the ply from the root.
     *
     * @param score the score to adjust.
     * @param plyFromRoot the ply from the root.
     * @return the adjusted mate score.
     */
    private int calculateMateScore(int score, int plyFromRoot) {
        return score > 0 ? score - plyFromRoot : score + plyFromRoot;
    }

    /**
     * Retrieves the mate score, adjusting it based on the ply from the root.
     *
     * @param score the score to adjust.
     * @param plyFromRoot the ply from the root.
     * @return the adjusted mate score.
     */
    private int retrieveMateScore(int score, int plyFromRoot) {
        return score > 0 ? score + plyFromRoot : score - plyFromRoot;
    }

}