package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.tables.pv.PrincipalVariationTable;

import java.time.Instant;

public class ThreadData {

    public final boolean mainThread;
    public int nodes;
    public int[][] nodesPerMove;
    public int depth;
    public PrincipalVariationTable pv;

    public ThreadData(boolean mainThread) {
        this.mainThread = mainThread;
        this.nodes = 0;
        this.nodesPerMove = new int[Square.COUNT][Square.COUNT];
        this.depth = 1;
        this.pv = new PrincipalVariationTable(Search.MAX_DEPTH);
    }

    public void addNodes(Move move, int nodes) {
        nodesPerMove[move.from()][move.to()] += nodes;
    }

    public int getNodes(Move move) {
        if (move == null) return 0;
        return nodesPerMove[move.from()][move.to()];
    }

    public boolean isMainThread() {
        return mainThread;
    }

    public void reset() {
        this.nodes = 0;
        this.nodesPerMove = new int[Square.COUNT][Square.COUNT];
        this.depth = 1;
        this.pv.clear();
    }

}
