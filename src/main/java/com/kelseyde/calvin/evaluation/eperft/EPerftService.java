package com.kelseyde.calvin.evaluation.eperft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.evaluation.Evaluator;
import com.kelseyde.calvin.movegeneration.MoveGenerator;

import java.util.List;

public class EPerftService {

    private final MoveGenerator moveGenerator;
    private final Evaluator evaluator;

    public EPerftService(Board board) {
        moveGenerator = new MoveGenerator();
        evaluator = new Evaluator(board);
    }

    public void ePerft(Board board, int depth) {
        if (depth == 0) {
            return;
        }
        List<Move> moves = moveGenerator.generateMoves(board, false);
        for (Move move : moves) {
            board.makeMove(move);
            evaluator.makeMove(move);
            ePerft(board, depth - 1);
            board.unmakeMove();
            evaluator.unmakeMove();
        }
    }

}
