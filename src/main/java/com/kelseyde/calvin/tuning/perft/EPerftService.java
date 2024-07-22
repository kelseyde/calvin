package com.kelseyde.calvin.tuning.perft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.Evaluator;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.notation.FEN;

import java.util.List;

public class EPerftService {

    private final MoveGenerator moveGenerator;
    private final Evaluator evaluator;
    private final NNUE nnue;

    public EPerftService(Board board) {
        moveGenerator = new MoveGenerator();
        evaluator = new Evaluator(EngineInitializer.loadDefaultConfig());
        nnue = new NNUE(board);
        evaluator.evaluate(board);
    }

    public void ePerft(Board board, int depth) {
        if (depth == 0) {
            return;
        }
        List<Move> moves = moveGenerator.generateMoves(board);
        for (Move move : moves) {
            nnue.makeMove(board, move);
            board.makeMove(move);
            int eval = evaluator.evaluate(board);
            int nnueEval = nnue.evaluate(board);
            if (Math.signum(eval) != Math.signum(nnueEval) && Math.abs(eval - nnueEval) > 600) {
//            if (board.isWhiteToMove()) {
                System.out.println("NNUE eval: " + nnueEval + " vs. eval: " + eval + "  " + FEN.toFEN(board));
            }
            ePerft(board, depth - 1);
            nnue.unmakeMove();
            board.unmakeMove();
        }
    }

}
