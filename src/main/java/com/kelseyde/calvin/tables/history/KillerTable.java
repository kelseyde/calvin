package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.search.Search;

public class KillerTable {

    public static final int KILLERS_PER_PLY = 2;

    Move[][] table = new Move[Search.MAX_DEPTH][KILLERS_PER_PLY];

    public void add(int ply, Move move) {
        if (ply >= Search.MAX_DEPTH) return;

        // Check if the move already exists in the killer list
        for (int i = 0; i < KILLERS_PER_PLY; i++) {
            if (move.equals(table[ply][i])) {
                // If the move exists at index i, shift all moves before it back by one position
                for (int j = i; j > 0; j--) {
                    table[ply][j] = table[ply][j - 1];
                }
                // Move the killer to the front
                table[ply][0] = move;
                return;
            }
        }

        // Shift the existing moves to make room for the new killer move
        for (int i = KILLERS_PER_PLY - 1; i > 0; i--) {
            table[ply][i] = table[ply][i - 1];
        }

        // Insert the new killer move at the start
        table[ply][0] = move;
    }

    public Move[] getKillers(int ply) {
        return table[ply];
    }

    public void clear(int ply) {
        if (ply >= Search.MAX_DEPTH) return;
        table[ply] = new Move[KILLERS_PER_PLY];
    }

    public void clear() {
        table = new Move[Search.MAX_DEPTH][KILLERS_PER_PLY];
    }

}
