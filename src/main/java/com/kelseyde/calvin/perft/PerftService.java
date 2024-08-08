package com.kelseyde.calvin.perft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.Notation;

import java.util.List;
import java.util.Set;

public class PerftService {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    public long totalNodeCount = 0;

    public long perft(Board board, int depth) {
        totalNodeCount++;
        List<Move> moves = moveGenerator.generateMoves(board);
        if (depth == 1) {
            int count = 0;
            for (Move move : moves) {
                board.makeMove(move);
                if (moveGenerator.isCheck(board, !board.isWhiteToMove())) {
                    board.unmakeMove();
                    continue;
                }
                count++;
                board.unmakeMove();
            }
            return count;
        }
        long totalMoveCount = 0;
        for (Move move : moves) {
            board.makeMove(move);
            if (moveGenerator.isCheck(board, !board.isWhiteToMove())) {
                board.unmakeMove();
                continue;
            }
            totalMoveCount += perft(board, depth - 1);
            board.unmakeMove();
        }
        return totalMoveCount;
    }

    private void log(Board board, int depth, Set<Move> moves) {
        List<String> moveHistory = board.getMoveHistory().stream().map(Notation::toNotation).toList();
        List<String> legalMoves = moves.stream().map(Notation::toNotation).toList();
        System.out.printf("perft(%s) -- %s: %s -- %s%n", depth, moveHistory, legalMoves.size(), legalMoves);
    }

    private void log(Board board, int depth, int count) {
        List<String> moveHistory = board.getMoveHistory().stream().map(Notation::toNotation).toList();
        System.out.printf("perft(%s) -- %s: %s %n", depth, moveHistory, count);
    }

}
