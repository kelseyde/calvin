package com.kelseyde.calvin.board;

import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CheckmateTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    @Test
    public void testFoolsMate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "f2", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g4"));

        board.makeMove(TestUtils.getLegalMove(board, "d8", "h4"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());
    }

    @Test
    public void testScholarsMate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "d1", "h5"));
        board.makeMove(TestUtils.getLegalMove(board, "b8", "c6"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "c4"));
        board.makeMove(TestUtils.getLegalMove(board, "d7", "d6"));

        board.makeMove(TestUtils.getLegalMove(board, "h5", "f7"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

    }

    @Test
    public void testDiscoveredCheckmate() {

        // Lasker vs Thomas, 1912
        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "d2", "d4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e6"));
        board.makeMove(TestUtils.getLegalMove(board, "g1", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "f7", "f5"));
        board.makeMove(TestUtils.getLegalMove(board, "b1", "c3"));
        board.makeMove(TestUtils.getLegalMove(board, "g8", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "c1", "g5"));
        board.makeMove(TestUtils.getLegalMove(board, "f8", "e7"));
        board.makeMove(TestUtils.getLegalMove(board, "g5", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "f5", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "c3", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "b7", "b6"));
        board.makeMove(TestUtils.getLegalMove(board, "f3", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "e8", "g8"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "d3"));
        board.makeMove(TestUtils.getLegalMove(board, "c8", "b7"));
        board.makeMove(TestUtils.getLegalMove(board, "d1", "h5"));
        board.makeMove(TestUtils.getLegalMove(board, "d8", "e7"));
        // now the fun begins
        board.makeMove(TestUtils.getLegalMove(board, "h5", "h7"));
        board.makeMove(TestUtils.getLegalMove(board, "g8", "h7"));
        board.makeMove(TestUtils.getLegalMove(board, "e4", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "h7", "h6"));
        board.makeMove(TestUtils.getLegalMove(board, "e5", "g4"));
        board.makeMove(TestUtils.getLegalMove(board, "h6", "g5"));
        board.makeMove(TestUtils.getLegalMove(board, "h2", "h4"));
        board.makeMove(TestUtils.getLegalMove(board, "g5", "f4"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g3"));
        board.makeMove(TestUtils.getLegalMove(board, "f4", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "d3", "e2"));
        board.makeMove(TestUtils.getLegalMove(board, "f3", "g2"));
        board.makeMove(TestUtils.getLegalMove(board, "h1", "h2"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g1"));

        board.makeMove(TestUtils.getLegalMove(board, "e1", "d2"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

    }

    @Test
    public void testCastlesCheckmate() {

        // Lasker vs Thomas, 1912
        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "d2", "d4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e6"));
        board.makeMove(TestUtils.getLegalMove(board, "g1", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "f7", "f5"));
        board.makeMove(TestUtils.getLegalMove(board, "b1", "c3"));
        board.makeMove(TestUtils.getLegalMove(board, "g8", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "c1", "g5"));
        board.makeMove(TestUtils.getLegalMove(board, "f8", "e7"));
        board.makeMove(TestUtils.getLegalMove(board, "g5", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "f5", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "c3", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "b7", "b6"));
        board.makeMove(TestUtils.getLegalMove(board, "f3", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "e8", "g8"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "d3"));
        board.makeMove(TestUtils.getLegalMove(board, "c8", "b7"));
        board.makeMove(TestUtils.getLegalMove(board, "d1", "h5"));
        board.makeMove(TestUtils.getLegalMove(board, "d8", "e7"));
        // now the fun begins
        board.makeMove(TestUtils.getLegalMove(board, "h5", "h7"));
        board.makeMove(TestUtils.getLegalMove(board, "g8", "h7"));
        board.makeMove(TestUtils.getLegalMove(board, "e4", "f6"));
        board.makeMove(TestUtils.getLegalMove(board, "h7", "h6"));
        board.makeMove(TestUtils.getLegalMove(board, "e5", "g4"));
        board.makeMove(TestUtils.getLegalMove(board, "h6", "g5"));
        board.makeMove(TestUtils.getLegalMove(board, "h2", "h4"));
        board.makeMove(TestUtils.getLegalMove(board, "g5", "f4"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g3"));
        board.makeMove(TestUtils.getLegalMove(board, "f4", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "d3", "e2"));
        board.makeMove(TestUtils.getLegalMove(board, "f3", "g2"));
        board.makeMove(TestUtils.getLegalMove(board, "h1", "h2"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g1"));

        // improving on Lasker's move, O-O-O#!
        board.makeMove(TestUtils.getLegalMove(board, "e1", "c1"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());


    }

    @Test
    public void testEnPassantCheckmate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "f7", "f5"));
        board.makeMove(TestUtils.getLegalMove(board, "e4", "f5"));
        board.makeMove(TestUtils.getLegalMove(board, "e8", "f7"));
        board.makeMove(TestUtils.getLegalMove(board, "b2", "b3"));
        board.makeMove(TestUtils.getLegalMove(board, "d7", "d6"));
        board.makeMove(TestUtils.getLegalMove(board, "c1", "b2"));
        board.makeMove(TestUtils.getLegalMove(board, "h7", "h6"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "b5"));
        board.makeMove(TestUtils.getLegalMove(board, "a7", "a6"));
        board.makeMove(TestUtils.getLegalMove(board, "d1", "g4"));
        board.makeMove(TestUtils.getLegalMove(board, "g7", "g5"));

        board.makeMove(TestUtils.getLegalMove(board, "f5", "g6"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

    }

    @Test
    public void testKnightPromotionCheckmate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "d2", "d3"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "e1", "d2"));
        board.makeMove(TestUtils.getLegalMove(board, "e5", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "d2", "c3"));
        board.makeMove(TestUtils.getLegalMove(board, "e4", "d3"));
        board.makeMove(TestUtils.getLegalMove(board, "b2", "b3"));
        board.makeMove(TestUtils.getLegalMove(board, "d3", "e2"));
        board.makeMove(TestUtils.getLegalMove(board, "c3", "b2"));

        Move move = new Move(Notation.fromNotation("e2"), Notation.fromNotation("d1"), Move.PROMOTE_TO_KNIGHT_FLAG);
        board.makeMove(move);
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

    }

    @Test
    public void testSimpleQueenCheckmate() {

        String fen = "8/k7/2K5/8/8/8/8/1Q6 w - - 0 1";
        Board board = FEN.toBoard(fen);

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

    }

    @Test
    public void testReturnsCheckmateResultForCorrectSide() {

        String fen = "8/k7/2K5/8/8/8/8/1Q6 w - - 0 1";
        Board board = FEN.toBoard(fen);

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());

        fen = "8/K7/2k5/8/8/8/8/1q6 b - - 0 1";
        board = FEN.toBoard(fen);

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        Assertions.assertTrue(moveGenerator.generateLegalMoves(board, MoveGeneration.MoveFilter.ALL).isEmpty());


    }

}
