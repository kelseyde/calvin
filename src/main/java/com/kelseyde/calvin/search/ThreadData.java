package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Move;
import lombok.Data;

import java.time.Instant;

@Data
public class ThreadData {

    public boolean mainThread;
    public Instant start;
    public int nodes;
    public int[][] nodesPerMove;
    public int depth;

    public ThreadData(boolean mainThread) {
        this.mainThread = mainThread;
        this.nodes = 0;
        this.nodesPerMove = new int[64][64];
        this.depth = 1;
    }

    public void setNodes(Move move, int nodes) {
        int from = move.getFrom();
        int to = move.getTo();
        nodesPerMove[from][to] = nodes;
    }

    public int getNodes(Move move) {
        if (move == null) return 0;
        int from = move.getFrom();
        int to = move.getTo();
        return nodesPerMove[from][to];
    }

    public void reset() {
        this.start = Instant.now();
        this.nodes = 0;
        this.nodesPerMove = new int[64][64];
        this.depth = 1;
    }

}
