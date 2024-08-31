package com.kelseyde.calvin.board;

import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.IllegalMoveException;
import com.kelseyde.calvin.utils.Notation;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardTest {

    @Test
    public void testFromPositionDoesNotCorruptBoard() {

        Board board = TestUtils.emptyBoard();
        assertSinglePieceBoard(board, 0);
        assertSinglePieceBoard(board, 7);
        assertSinglePieceBoard(board, 12);
        assertSinglePieceBoard(board, 18);
        assertSinglePieceBoard(board, 25);
        assertSinglePieceBoard(board, 31);
        assertSinglePieceBoard(board, 38);
        assertSinglePieceBoard(board, 36);
        assertSinglePieceBoard(board, 43);
        assertSinglePieceBoard(board, 54);
        assertSinglePieceBoard(board, 59);
        assertSinglePieceBoard(board, 60);
        assertSinglePieceBoard(board, 63);

    }

    @Test
    public void testBoardHistoryPreservedMultipleMoves() {
        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "g1", "f3"));
        board.makeMove(TestUtils.getLegalMove(board, "b8", "c6"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "b5"));
        board.makeMove(TestUtils.getLegalMove(board, "a7", "a6"));
        board.makeMove(TestUtils.getLegalMove(board, "b5", "a4"));

        Assertions.assertEquals(7, board.getMoveHistory().size());

    }

    @Test
    public void testBoardHistoryPreservesCastlingRights() {

        Board board = new Board();
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e3"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e6"));

        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

        board.makeMove(TestUtils.getLegalMove(board, "e1", "e2"));

        Assertions.assertFalse(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertFalse(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

        Assertions.assertTrue(board.getGameStateHistory().peek().isKingsideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameStateHistory().peek().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameStateHistory().peek().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameStateHistory().peek().isQueensideCastlingAllowed(false));

        board.makeMove(TestUtils.getLegalMove(board, "f7", "f6"));

        Assertions.assertFalse(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertFalse(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

        Assertions.assertFalse(board.getGameStateHistory().peek().isKingsideCastlingAllowed(true));
        Assertions.assertFalse(board.getGameStateHistory().peek().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameStateHistory().peek().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameStateHistory().peek().isQueensideCastlingAllowed(false));

    }

    @Test
    public void testSimpleUnmakeMove() {
        Board board1 = new Board();
        board1.makeMove(new Move(12, 28));
        board1.makeMove(new Move(52, 36));
        board1.makeMove(new Move(11, 27));
        board1.makeMove(new Move(51, 35));
        board1.makeMove(new Move(10, 26));
        board1.makeMove(new Move(50, 34));
        board1.makeMove(new Move(9, 25));
        board1.makeMove(new Move(49, 33));
        board1.makeMove(new Move(8, 24));
        board1.makeMove(new Move(48, 32));
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();
        board1.unmakeMove();

        Board board2 = new Board();

        Assertions.assertEquals(board1.getPawns(true), board2.getPawns(true));
        Assertions.assertEquals(board1.getKnights(true), board2.getKnights(true));
        Assertions.assertEquals(board1.getBishops(true), board2.getBishops(true));
        Assertions.assertEquals(board1.getRooks(true), board2.getRooks(true));
        Assertions.assertEquals(board1.getQueens(true), board2.getQueens(true));
        Assertions.assertEquals(board1.getKing(true), board2.getKing(true));
        Assertions.assertEquals(board1.getPawns(false), board2.getPawns(false));
        Assertions.assertEquals(board1.getKnights(false), board2.getKnights(false));
        Assertions.assertEquals(board1.getBishops(false), board2.getBishops(false));
        Assertions.assertEquals(board1.getRooks(false), board2.getRooks(false));
        Assertions.assertEquals(board1.getQueens(false), board2.getQueens(false));
        Assertions.assertEquals(board1.getKing(false), board2.getKing(false));
        Assertions.assertEquals(board1.getWhitePieces(), board2.getWhitePieces());
        Assertions.assertEquals(board1.getBlackPieces(), board2.getBlackPieces());
        Assertions.assertEquals(board1.getOccupied(), board2.getOccupied());
        Assertions.assertEquals(board1.isWhiteToMove(), board2.isWhiteToMove());

        Assertions.assertEquals(board1.getGameState(), board2.getGameState());

    }

    @Test
    public void testEnPassantFileIsClearedAfterNextMove() {
        // TODO
        Board board1 = new Board();
        board1.makeMove(new Move(13, 21));
        board1.makeMove(new Move(51, 35, Move.PAWN_DOUBLE_MOVE_FLAG));

        new MoveGenerator().generateMoves(board1);
    }

    @Test
    public void testGenerateLegalMovesDoesNotCorruptBoard() {

        Board board1 = new Board();
        Board board2 = new Board();

        new MoveGenerator().generateMoves(board1);

        Assertions.assertEquals(board1.getPawns(true), board2.getPawns(true));
        Assertions.assertEquals(board1.getKnights(true), board2.getKnights(true));
        Assertions.assertEquals(board1.getBishops(true), board2.getBishops(true));
        Assertions.assertEquals(board1.getRooks(true), board2.getRooks(true));
        Assertions.assertEquals(board1.getQueens(true), board2.getQueens(true));
        Assertions.assertEquals(board1.getKing(true), board2.getKing(true));
        Assertions.assertEquals(board1.getPawns(false), board2.getPawns(false));
        Assertions.assertEquals(board1.getKnights(false), board2.getKnights(false));
        Assertions.assertEquals(board1.getBishops(false), board2.getBishops(false));
        Assertions.assertEquals(board1.getRooks(false), board2.getRooks(false));
        Assertions.assertEquals(board1.getQueens(false), board2.getQueens(false));
        Assertions.assertEquals(board1.getKing(false), board2.getKing(false));
        Assertions.assertEquals(board1.getWhitePieces(), board2.getWhitePieces());
        Assertions.assertEquals(board1.getBlackPieces(), board2.getBlackPieces());
        Assertions.assertEquals(board1.getOccupied(), board2.getOccupied());
        Assertions.assertEquals(board1.isWhiteToMove(), board2.isWhiteToMove());

        Assertions.assertEquals(board1.getGameState(), board2.getGameState());

    }

    @Test
    public void testUnmakeMoveRestoresCapturedPieces() {

        Board board = new Board();
        board.makeMove(new Move(12, 28));
        board.makeMove(new Move(51, 35));
        board.makeMove(new Move(28, 35));

        Set<Integer> whitePiecePositions = getPiecePositions(board, true);
        Assertions.assertEquals(Set.of(35, 8, 9, 10, 11, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7), whitePiecePositions);

        Set<Integer> blackPiecePositions = getPiecePositions(board, false);
        Assertions.assertEquals(Set.of(56, 57, 58, 59, 60, 61, 62, 63, 48, 49, 50, 52, 53, 54, 55), blackPiecePositions);

        board.unmakeMove();

        whitePiecePositions = getPiecePositions(board, true);
        Assertions.assertEquals(Set.of(28, 8, 9, 10, 11, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7), whitePiecePositions);

        blackPiecePositions = getPiecePositions(board, false);
        Assertions.assertEquals(Set.of(35, 56, 57, 58, 59, 60, 61, 62, 63, 48, 49, 50, 52, 53, 54, 55), blackPiecePositions);

    }

    @Test
    public void testUnmakeEnPassantRestoresCapturedPawn() {

        Board board = new Board();
        //d4d5
        board.makeMove(new Move(11, 27));
        //e7e5
        board.makeMove(new Move(52, 36));
        //d4e5
        board.makeMove(new Move(27, 36));
        //d7d5
        board.makeMove(new Move(51, 35, Move.PAWN_DOUBLE_MOVE_FLAG));
        //e5d6
        board.makeMove(new Move(36, 43, Move.EN_PASSANT_FLAG));

        Set<Integer> blackPiecePositions = getPiecePositions(board, false);
        Assertions.assertFalse(blackPiecePositions.contains(35));

        board.unmakeMove();
        blackPiecePositions = getPiecePositions(board, false);
        Assertions.assertTrue(blackPiecePositions.contains(35));

    }

    @Test
    public void testUnmakeMoveRemovesCorrectMoveFromMoveHistory() {

        Board board = new Board();
        board.makeMove(new Move(12, 28));
        board.makeMove(new Move(51, 35));
        board.makeMove(new Move(28, 35));

        List<Move> moveHistory = board.getMoveHistory().stream().toList();
        Assertions.assertEquals(3, moveHistory.size());
        Assertions.assertTrue(new Move(28, 35).matches(moveHistory.get(0)));
        Assertions.assertTrue(new Move(51, 35).matches(moveHistory.get(1)));
        Assertions.assertTrue(new Move(12, 28).matches(moveHistory.get(2)));

        board.unmakeMove();

        moveHistory = board.getMoveHistory().stream().toList();
        Assertions.assertEquals(2, moveHistory.size());
        Assertions.assertTrue(new Move(51, 35).matches(moveHistory.get(0)));
        Assertions.assertTrue(new Move(12, 28).matches(moveHistory.get(1)));

    }

    @Test
    public void testUnmakeMoveHandlesTurnSwitching() {

        Board board = new Board();
        Assertions.assertTrue(board.isWhiteToMove());

        board.makeMove(new Move(12, 28));
        Assertions.assertFalse(board.isWhiteToMove());

        board.makeMove(new Move(51, 35));
        Assertions.assertTrue(board.isWhiteToMove());

        board.makeMove(new Move(28, 35));
        Assertions.assertFalse(board.isWhiteToMove());

        board.unmakeMove();
        Assertions.assertTrue(board.isWhiteToMove());

    }

    @Test
    public void testUnmakeMoveHandlesCastling() {

        Board board = new Board();
        board.makeMove(new Move(12, 28));
        board.makeMove(new Move(52, 44));
        board.makeMove(new Move(6, 21));
        board.makeMove(new Move(62, 45));
        board.makeMove(new Move(5, 12));
        board.makeMove(new Move(61, 52));
        // castles
        board.makeMove(new Move(4, 6, Move.CASTLE_FLAG));
        Assertions.assertFalse(board.isWhiteToMove());
        Assertions.assertFalse(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertFalse(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

        board.unmakeMove();
        Assertions.assertTrue(board.isWhiteToMove());
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(true));
        Assertions.assertTrue(board.getGameState().isKingsideCastlingAllowed(false));
        Assertions.assertTrue(board.getGameState().isQueensideCastlingAllowed(false));

    }

    @Test
    public void testUnmakeCheckmate() {

        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        board.makeMove(TestUtils.getLegalMove(board, "d1", "h5"));
        board.makeMove(TestUtils.getLegalMove(board, "b8", "c6"));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "c4"));
        board.makeMove(TestUtils.getLegalMove(board, "g8", "f6"));
        // scholar's mate
        board.makeMove(TestUtils.getLegalMove(board, "h5", "f7"));
        Assertions.assertFalse(board.isWhiteToMove());
        // todo

    }

    @Test
    public void testRookCannotJumpToOtherSide() {

        String fen = "r1b1k2r/1p3p2/8/8/1n6/2Q5/4P2p/5KNR w kq - 0 1";
        Board board = FEN.toBoard(fen);
        board.makeMove(TestUtils.getLegalMove(board, "c3", "b4"));
        Move queenPromotion = Notation.fromNotation("h2", "g1", Move.PROMOTE_TO_QUEEN_FLAG);
        board.makeMove(TestUtils.getLegalMove(board, queenPromotion));
        board.makeMove(TestUtils.getLegalMove(board, "f1", "g1"));
        board.makeMove(TestUtils.getLegalMove(board, "h8", "h1"));
        board.makeMove(TestUtils.getLegalMove(board, "g1", "h1"));
        Assertions.assertThrows(IllegalMoveException.class, () ->
                board.makeMove(TestUtils.getLegalMove(board, "h8", "h1")));
    }

    @Test
    public void testKnightMoveGenerationBug() {
        Board board = new Board();
        board.makeMove(TestUtils.getLegalMove(board, "b1", "a3"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        List<String> moves = new MoveGenerator().generateMoves(board).stream().map(Notation::toNotation).toList();
        System.out.println(moves.size());
        System.out.println(moves);
        Assertions.assertEquals(20, moves.size());
    }

    @Test
    public void testMakeNullMoveChangesSideToMove() {

        Board board = FEN.toBoard("rn1qkb1r/ppp2ppp/3p1n2/8/2BPPpb1/5N2/PPP3PP/RNBQK2R w KQkq - 1 6");
        long initialZobrist = board.getGameState().getZobrist();
        Assertions.assertTrue(board.isWhiteToMove());
        board.makeNullMove();
        Assertions.assertFalse(board.isWhiteToMove());
        Board board2 = FEN.toBoard("rn1qkb1r/ppp2ppp/3p1n2/8/2BPPpb1/5N2/PPP3PP/RNBQK2R b KQkq - 1 6");
        Assertions.assertEquals(board.getGameState().getZobrist(), board2.getGameState().getZobrist());
        board.unmakeNullMove();
        Assertions.assertTrue(board.isWhiteToMove());
        Assertions.assertEquals(initialZobrist, board.getGameState().getZobrist());

    }

    @Test
    public void testUnmakeMoveResetsEnPassantFile() {

        Board board = FEN.toBoard("r1bqkbnr/ppp1pppp/2n5/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");
        long initialZobrist = board.getGameState().getZobrist();
        Assertions.assertEquals(3, board.getGameState().getEnPassantFile());
        board.makeNullMove();
        Assertions.assertEquals(-1, board.getGameState().getEnPassantFile());
        board.unmakeNullMove();
        Assertions.assertEquals(3, board.getGameState().getEnPassantFile());
        Assertions.assertEquals(initialZobrist, board.getGameState().getZobrist());

    }

    @Test
    public void testUnmakeMoveResetsFiftyMoveCounter() {

        Board board = FEN.toBoard("8/4n3/2kn4/8/3B4/5K2/8/8 w - - 4 3");
        long initialZobrist = board.getGameState().getZobrist();
        Assertions.assertEquals(4, board.getGameState().getHalfMoveClock());
        board.makeNullMove();
        Assertions.assertEquals(0, board.getGameState().getHalfMoveClock());
        board.unmakeNullMove();
        Assertions.assertEquals(4, board.getGameState().getHalfMoveClock());
        Assertions.assertEquals(initialZobrist, board.getGameState().getZobrist());

    }

    @Test
    public void testPromotionWithFilters() {

        String fen = "8/k5P1/8/8/8/8/5rr1/7K w - - 0 1";
        Board board = FEN.toBoard(fen);

        MoveGenerator moveGenerator = new MoveGenerator();

        Assertions.assertEquals(0, moveGenerator.generateMoves(board, MoveGeneration.MoveFilter.QUIET).size());
        Assertions.assertEquals(4, moveGenerator.generateMoves(board, MoveGeneration.MoveFilter.NOISY).size());

    }

    @Test
    public void testUnmakeCastling() {

        Board board = new Board();
        board.makeMove(Notation.fromUCI("e2e4"));
        board.makeMove(Notation.fromUCI("d7d5"));
        board.makeMove(Notation.fromUCI("g1f3"));
        board.makeMove(Notation.fromUCI("b8c6"));
        board.makeMove(Notation.fromUCI("f1b5"));
        board.makeMove(Notation.fromUCI("c8g4"));
        board.makeMove(Notation.fromNotation("e1", "g1", Move.CASTLE_FLAG));
        board.makeMove(Notation.fromUCI("d8d7"));
        board.makeMove(Notation.fromNotation("f1", "e1"));
        board.makeMove(Notation.fromNotation("e8","c8",Move.CASTLE_FLAG));

        Board board2 = FEN.toBoard("2kr1bnr/pppqpppp/2n5/1B1p4/4P1b1/5N2/PPPP1PPP/RNBQR1K1 w - - 8 6");
        Assertions.assertEquals(board.getWhitePieces(), board2.getWhitePieces());
        Assertions.assertEquals(board.getBlackPieces(), board2.getBlackPieces());
        Assertions.assertEquals(board.getRooks(), board2.getRooks());

        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();
        board.unmakeMove();

        Board board3 = new Board();
        Assertions.assertEquals(board.getWhitePieces(), board3.getWhitePieces());
        Assertions.assertEquals(board.getBlackPieces(), board3.getBlackPieces());
        Assertions.assertEquals(board.getRooks(), board3.getRooks());

    }

    private Set<Integer> getPiecePositions(Board board, boolean whiteToMove) {
        Set<Integer> positions = new HashSet<>();
        if (whiteToMove) {
            long whitePieces = board.getWhitePieces();
            while (whitePieces != 0) {
                int position = Bitwise.getNextBit(whitePieces);
                positions.add(position);
                whitePieces = Bitwise.popBit(whitePieces);
            }
        } else {
            long blackPieces = board.getBlackPieces();
            while (blackPieces != 0) {
                int position = Bitwise.getNextBit(blackPieces);
                positions.add(position);
                blackPieces = Bitwise.popBit(blackPieces);
            }
        }
        return positions;
    }

    private void assertSinglePieceBoard(Board board, int startSquare) {
        board.toggleSquare(Piece.ROOK, true, startSquare);
        Assertions.assertEquals(Set.of(startSquare), getPiecePositions(board, true));
        Assertions.assertEquals(Set.of(), getPiecePositions(board, false));
        board.toggleSquare(Piece.ROOK, true, startSquare);
    }

}
