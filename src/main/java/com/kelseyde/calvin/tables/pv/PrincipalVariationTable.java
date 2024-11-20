package com.kelseyde.calvin.tables.pv;

import com.kelseyde.calvin.board.Move;

public class PrincipalVariationTable {

    private final Move[][] table;
    private final int[] length;
    private final int ply;

    public PrincipalVariationTable(int ply) {
        this.table = new Move[ply][ply];
        this.length = new int[ply];
        this.ply = ply;
    }

    public Move[] getPrincipalVariation() {
        Move[] pv = new Move[length[0]];
        System.arraycopy(table[0], 0, pv, 0, length[0]);
        return pv;
    }

    public void update(int ply, Move move) {
        table[ply][0] = move;
        System.arraycopy(table[ply + 1], 0, table[ply], 1, length[ply + 1]);
        length[ply] = Math.min(length[ply + 1] + 1, this.ply);
    }

    public void resetLength(int ply) {
        length[ply] = 0;
    }

    public void clear() {
        for (int i = 0; i < ply; i++) {
            length[i] = 0;
        }
    }

}
