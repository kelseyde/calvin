package com.kelseyde.calvin.generation.drawcalculator;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.Score;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DrawByStalemateTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    @Test
    public void testSimpleQueenStalemate() {

        String fen = "k7/2K5/8/8/8/8/8/1Q6 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b6"));

        // king stalemated in the corner
        Assertions.assertTrue(moveGenerator.generateMoves(board).isEmpty());

    }

    @Test
    public void testSimpleKingAndPawnStalemate() {

        Board board = FEN.toBoard("4k3/4P3/3K4/8/8/8/8/8 w - - 0 1");
        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "d6", "e6"));

        // king stalemated by king and pawn
        Assertions.assertTrue(moveGenerator.generateMoves(board).isEmpty());

    }

    @Test
    public void testSimpleKingAndBishopStalemate() {

        Board board = FEN.toBoard("7k/8/6KP/5B2/8/8/8/8 w - - 0 1");

        board.makeMove(TestUtils.getLegalMove(board, "f5", "e6"));

        // king stalemated in the corner
        Assertions.assertTrue(moveGenerator.generateMoves(board).isEmpty());

    }

    @Test
    public void testStalemateWithPinnedPawn() {

        Board board = FEN.toBoard("7k/6p1/7P/4BBK1/8/8/1Q6/8 w - - 0 1");
        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "b2", "a2"));

        // even though pawn could pseudo-legally capture on h6 with check, it is pinned, therefore stalemate
        Assertions.assertTrue(moveGenerator.generateMoves(board).isEmpty());

    }

}
