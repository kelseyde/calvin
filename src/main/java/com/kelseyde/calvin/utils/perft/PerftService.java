package com.kelseyde.calvin.utils.perft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveList;
import com.kelseyde.calvin.uci.UCI;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerftService {

    private final MoveGenerator movegen = new MoveGenerator();

    public long nodesSearched = 0;
    private Map<Move, Long> nodesPerMove;

    public long perft(Board board, int depth) {
        nodesSearched = 0;
        nodesPerMove = new HashMap<>();

        long totalNodes = perft(board, depth, depth);

        nodesPerMove.entrySet().stream()
                .sorted(Comparator.comparing(entry -> Move.toUCI(entry.getKey())))
                .forEach(entry -> UCI.write(String.format("%s: %s", Move.toUCI(entry.getKey()), entry.getValue())));
        UCI.write(String.format("Nodes searched: %s", totalNodes));

        return totalNodes;
    }

    public long perft(Board board, int depth, int originalDepth) {
        nodesSearched++;
        MoveList moves = movegen.generateMoves(board);
        if (depth == 1) {
            return moves.size();
        }
        long totalMoveCount = 0;
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            board.makeMove(move);
            totalMoveCount += perft(board, depth - 1, originalDepth);
            if (depth == originalDepth) {
                nodesPerMove.put(move, totalMoveCount);
            }
            board.unmakeMove();
        }
        return totalMoveCount;
    }

}
