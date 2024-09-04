package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.search.Search;

public class KillerTable {

    public static final int KILLERS_PER_PLY = 3;

    Move[][] table = new Move[Search.MAX_DEPTH][KILLERS_PER_PLY];

    public Move get(int ply, int index) {
        return table[ply][index];
    }

    public void add(int ply, Move move) {
        if (ply >= Search.MAX_DEPTH) {
            return;
        }

        // Check if the move already exists in the killer moves list
        int existingIndex = -1;
        for (int i = 0; i < KILLERS_PER_PLY; i++) {
            if (move.equals(table[ply][i])) {
                existingIndex = i;
                break;
            }
        }

        // Shift all killers forward by one
        int startIndex = existingIndex == -1 ? KILLERS_PER_PLY - 1 : existingIndex;
        for (int j = startIndex; j > 0; j--) {
            table[ply][j] = table[ply][j - 1];
        }

        // Insert the new move at the front
        table[ply][0] = move;
    }

    public int score(Move move, int ply, int base, int bonus) {
        if (ply >= Search.MAX_DEPTH) {
            return 0;
        }
        for (int i = 0; i < KILLERS_PER_PLY; i++) {
            if (move.equals(table[ply][i])) {
                return base + (bonus * (KILLERS_PER_PLY - i));
            }
        }
        return 0;
    }

    public void clear() {
        table = new Move[Search.MAX_DEPTH][KILLERS_PER_PLY];
    }

}
