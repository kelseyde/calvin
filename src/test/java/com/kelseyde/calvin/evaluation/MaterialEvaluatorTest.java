package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.evaluation.material.MaterialCalculator;
import com.kelseyde.calvin.utils.NotationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MaterialEvaluatorTest {

    private final MaterialCalculator evaluator = new MaterialCalculator();

    @Test
    public void testStartingPosition() {

        Board board = new Board();
        int whiteScore = evaluator.calculate(board, true).eval();
        int blackScore = evaluator.calculate(board, false).eval();
        Assertions.assertEquals(0, whiteScore - blackScore);

    }

    @Test
    public void testManyBlunderedPieces() {

        Board board = new Board();
        board.makeMove(NotationUtils.fromNotation("e2", "e4"));
        board.makeMove(NotationUtils.fromNotation("e7", "e5"));
        board.makeMove(NotationUtils.fromNotation("g1", "f3"));
        board.makeMove(NotationUtils.fromNotation("f8", "a3"));
        board.makeMove(NotationUtils.fromNotation("b1", "a3"));
        board.makeMove(NotationUtils.fromNotation("d8", "g5"));
        board.makeMove(NotationUtils.fromNotation("f3", "g5"));
        board.makeMove(NotationUtils.fromNotation("f7", "f6"));
        board.makeMove(NotationUtils.fromNotation("f1", "a6"));
        board.makeMove(NotationUtils.fromNotation("b8", "a6"));
        board.makeMove(NotationUtils.fromNotation("e1", "f1"));
        board.makeMove(NotationUtils.fromNotation("f6", "g5"));
        board.makeMove(NotationUtils.fromNotation("d1", "h5"));
        board.makeMove(NotationUtils.fromNotation("g7", "g6"));
        board.makeMove(NotationUtils.fromNotation("d2", "d3"));
        board.makeMove(NotationUtils.fromNotation("g6", "h5"));

        // white score: (8 * 100) + 650 + 1000 + 10000 = 12450
        // black score: (8 * 100) + 970 + 1000 + 10000 = 12770
        // score = 12450 - 12770 = -320
        int whiteScore = evaluator.calculate(board, true).eval();
        int blackScore = evaluator.calculate(board, false).eval();
        Assertions.assertEquals(-320, whiteScore - blackScore);

    }

}