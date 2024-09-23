package com.kelseyde.calvin.tables.tt;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.evaluation.Score;

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

    private int tableSize;
    private HashEntry[] entries;
    private final int bucketSize;
    private int tries;
    private int hits;
    private int age;

    public TranspositionTable(int tableSizeMb, int bucketSize) {
        this.tableSize = (tableSizeMb * 1024 * 1024) / HashEntry.SIZE_BYTES;
        this.bucketSize = bucketSize;
        this.entries = new HashEntry[tableSize];
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
        long keyPart = HashEntry.keyPart(key);
        tries++;
        for (int i = 0; i < bucketSize; i++) {
            HashEntry entry = entries[index + i];
            if (entry != null && entry.getKeyPart() == keyPart) {
                hits++;
                entry.setAge(age);
                if (Score.isMateScore(entry.getScore())) {
                    int score = Score.readMateScore(entry.getScore(), ply);
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
     */
    public void put(long zobristKey, HashFlag flag, int depth, int ply, Move move, int staticEval, int score) {

        // Get the start index of the 4-item bucket.
        int startIndex = index(zobristKey);

        // If the eval is checkmate, adjust the score to reflect the number of ply from the root position
        if (Score.isMateScore(score)) score = Score.writeMateScore(score, ply);

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
            if (storedEntry.getKeyPart() == HashEntry.keyPart(zobristKey)) {
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
            if (age > storedEntry.getAge()) {
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
            entries[replacedIndex] = HashEntry.of(zobristKey, score, staticEval, move, flag, depth, age);
        }
    }

    public void incrementAge() {
        age++;
    }

    public void resize(int tableSizeMb) {
        this.tableSize = (tableSizeMb * 1024 * 1024) / HashEntry.SIZE_BYTES;
        entries = new HashEntry[tableSize];
        tries = 0;
        hits = 0;
        age = 0;
    }

    public void clear() {
        tries = 0;
        hits = 0;
        age = 0;
        entries = new HashEntry[tableSize];
    }

    /**
     * Compresses the 64-bit zobrist key into a 32-bit key, to be used as an index in the hash table.
     */
    private int index(long key) {
        // XOR the upper and lower halves of the zobrist key together, producing a pseudo-random 32-bit result.
        // Then apply a mask ensuring the number is always positive, since it is to be used as an array index.
        long index = Bits.abs(key ^ (key >>> 32));

        // Modulo the result with the number of entries in the table, and align it with a multiple of bucketSize,
        // ensuring the entries are always divided into 'bucketSize'-sized buckets.
        return (int) (index % (tableSize - (bucketSize - 1))) & -bucketSize;
    }

}