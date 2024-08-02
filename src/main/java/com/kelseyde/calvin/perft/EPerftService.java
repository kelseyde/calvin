package com.kelseyde.calvin.perft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;

import java.util.List;

public class EPerftService {

    private final MoveGenerator moveGenerator;
    private final Evaluation evaluator;

    public EPerftService(Board board) {
        moveGenerator = new MoveGenerator();
        evaluator = new NNUE(board);
        evaluator.evaluate();
    }

    public void ePerft(Board board, int depth) {
        if (depth == 0) {
            return;
        }
        List<Move> moves = moveGenerator.generateMoves(board);
        for (Move move : moves) {
            evaluator.makeMove(board, move);
            board.makeMove(move);
            if (evaluator.evaluate() != new NNUE(board).evaluate()) {
                System.out.println("NNUE evaluation mismatch!" + evaluator.evaluate() + " " + new NNUE(board).evaluate() + " " + FEN.toFEN(board) + " " + Notation.toNotation(move));
            }
            ePerft(board, depth - 1);
            evaluator.unmakeMove();
            board.unmakeMove();
            if (evaluator.evaluate() != new NNUE(board).evaluate()) {
                System.out.println("NNUE evaluation mismatch!" + evaluator.evaluate() + " " + new NNUE(board).evaluate() + " " + FEN.toFEN(board) + " " + Notation.toNotation(move));
            }
        }
    }

}
