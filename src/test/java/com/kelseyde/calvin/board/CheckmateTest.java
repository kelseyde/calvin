package com.kelseyde.calvin.board;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.move.Move;
import com.kelseyde.calvin.board.move.MoveType;
import com.kelseyde.calvin.board.piece.PieceType;
import com.kelseyde.calvin.movegeneration.result.GameResult;
import com.kelseyde.calvin.movegeneration.result.ResultCalculator;
import com.kelseyde.calvin.utils.NotationUtils;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CheckmateTest {
    
    private final ResultCalculator evaluator = new ResultCalculator();

    @Test
    public void testFoolsMate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "f2", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "g2", "g4"));

        board.makeMove(TestUtils.getLegalMove(board, "d8", "h4"));
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());

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
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());

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
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());

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
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());

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
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());
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

        Move move = Move.builder()
                .startSquare(NotationUtils.fromNotation("e2"))
                .endSquare(NotationUtils.fromNotation("d1"))
                .pieceType(PieceType.PAWN)
                .moveType(MoveType.PROMOTION)
                .promotionPieceType(PieceType.KNIGHT)
                .build();
        board.makeMove(move);
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());
    }

    @Test
    public void testSimpleQueenCheckmate() {

        Board board = TestUtils.emptyBoard();
        board.setPiece(48, PieceType.KING, false, true);
        board.setPiece(42, PieceType.KING, true, true);
        board.setPiece(1, PieceType.QUEEN, true, true);
        
        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertTrue(result.isCheckmate());

    }

    @Test
    public void testReturnsCheckmateResultForCorrectSide() {

        Board board = TestUtils.emptyBoard();
        board.setPiece(48, PieceType.KING, false, true);
        board.setPiece(42, PieceType.KING, true, true);
        board.setPiece(1, PieceType.QUEEN, true, true);

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        GameResult result = evaluator.calculateResult(board);
        Assertions.assertEquals(GameResult.WHITE_WINS_BY_CHECKMATE, result);

        board = TestUtils.emptyBoard();
        board.setWhiteToMove(false);
        board.setPiece(48, PieceType.KING, true, true);
        board.setPiece(42, PieceType.KING, false, true);
        board.setPiece(1, PieceType.QUEEN, false, true);

        board.makeMove(TestUtils.getLegalMove(board, "b1", "b7"));
        result = evaluator.calculateResult(board);
        Assertions.assertEquals(GameResult.BLACK_WINS_BY_CHECKMATE, result);

    }

}
