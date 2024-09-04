package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class MoveOrdererTest {

    private MoveOrderer moveOrderer;

    private final MoveGenerator moveGenerator = new MoveGenerator();

    @BeforeEach
    public void beforeEach() {
        moveOrderer = new MoveOrderer();
    }

    @Test
    public void testMvvLvaScores() {

        int[][] table = MoveOrderer.MVV_LVA_TABLE;

        Assertions.assertEquals(10, table[Piece.PAWN.getIndex()][Piece.KING.getIndex()]);
        Assertions.assertEquals(11, table[Piece.PAWN.getIndex()][Piece.QUEEN.getIndex()]);
        Assertions.assertEquals(12, table[Piece.PAWN.getIndex()][Piece.ROOK.getIndex()]);
        Assertions.assertEquals(13, table[Piece.PAWN.getIndex()][Piece.BISHOP.getIndex()]);
        Assertions.assertEquals(14, table[Piece.PAWN.getIndex()][Piece.KNIGHT.getIndex()]);
        Assertions.assertEquals(15, table[Piece.PAWN.getIndex()][Piece.PAWN.getIndex()]);
        Assertions.assertEquals(20, table[Piece.KNIGHT.getIndex()][Piece.KING.getIndex()]);
        Assertions.assertEquals(21, table[Piece.KNIGHT.getIndex()][Piece.QUEEN.getIndex()]);
        Assertions.assertEquals(22, table[Piece.KNIGHT.getIndex()][Piece.ROOK.getIndex()]);
        Assertions.assertEquals(23, table[Piece.KNIGHT.getIndex()][Piece.BISHOP.getIndex()]);
        Assertions.assertEquals(24, table[Piece.KNIGHT.getIndex()][Piece.KNIGHT.getIndex()]);
        Assertions.assertEquals(25, table[Piece.KNIGHT.getIndex()][Piece.PAWN.getIndex()]);
        Assertions.assertEquals(30, table[Piece.BISHOP.getIndex()][Piece.KING.getIndex()]);
        Assertions.assertEquals(31, table[Piece.BISHOP.getIndex()][Piece.QUEEN.getIndex()]);
        Assertions.assertEquals(32, table[Piece.BISHOP.getIndex()][Piece.ROOK.getIndex()]);
        Assertions.assertEquals(33, table[Piece.BISHOP.getIndex()][Piece.BISHOP.getIndex()]);
        Assertions.assertEquals(34, table[Piece.BISHOP.getIndex()][Piece.KNIGHT.getIndex()]);
        Assertions.assertEquals(35, table[Piece.BISHOP.getIndex()][Piece.PAWN.getIndex()]);
        Assertions.assertEquals(40, table[Piece.ROOK.getIndex()][Piece.KING.getIndex()]);
        Assertions.assertEquals(41, table[Piece.ROOK.getIndex()][Piece.QUEEN.getIndex()]);
        Assertions.assertEquals(42, table[Piece.ROOK.getIndex()][Piece.ROOK.getIndex()]);
        Assertions.assertEquals(43, table[Piece.ROOK.getIndex()][Piece.BISHOP.getIndex()]);
        Assertions.assertEquals(44, table[Piece.ROOK.getIndex()][Piece.KNIGHT.getIndex()]);
        Assertions.assertEquals(45, table[Piece.ROOK.getIndex()][Piece.PAWN.getIndex()]);
        Assertions.assertEquals(50, table[Piece.QUEEN.getIndex()][Piece.KING.getIndex()]);
        Assertions.assertEquals(51, table[Piece.QUEEN.getIndex()][Piece.QUEEN.getIndex()]);
        Assertions.assertEquals(52, table[Piece.QUEEN.getIndex()][Piece.ROOK.getIndex()]);
        Assertions.assertEquals(53, table[Piece.QUEEN.getIndex()][Piece.BISHOP.getIndex()]);
        Assertions.assertEquals(54, table[Piece.QUEEN.getIndex()][Piece.KNIGHT.getIndex()]);
        Assertions.assertEquals(55, table[Piece.QUEEN.getIndex()][Piece.PAWN.getIndex()]);

    }

    @Test
    public void testLosingCaptureBias() {

        String fen = "7k/8/8/8/4r3/3P1Q2/8/7K w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("h1", "h2"),
                Notation.fromNotation("h1", "g1"),
                Notation.fromNotation("h1", "g2"),
                Notation.fromNotation("f3", "e4")
//                NotationUtils.fromNotation("d3", "e4", PieceType.PAWN),
        ));

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(Notation.fromNotation("f3", "e4")));

    }

    @Test
    public void testKillerMoveBias() {

        String fen = "k7/8/8/6q1/8/5PP1/6BP/6K1 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                TestUtils.getLegalMove(board, "g1", "h1"),
                TestUtils.getLegalMove(board, "g1", "f1"),
                TestUtils.getLegalMove(board, "f3", "f4"),
                TestUtils.getLegalMove(board, "h2", "h4")));

        Move killerMove1 = TestUtils.getLegalMove(board, "f3", "f4");
        moveOrderer.addKillerMove(1, killerMove1);

        Move killerMove2 = TestUtils.getLegalMove(board, "h2", "h4");
        moveOrderer.addKillerMove(1, killerMove2);

        Move killerMove3 = TestUtils.getLegalMove(board, "g1", "f1");
        moveOrderer.addKillerMove(1, killerMove3);

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(killerMove3));
        Assertions.assertTrue(orderedMoves.get(1).matches(killerMove2));
        Assertions.assertTrue(orderedMoves.get(2).matches(killerMove1));


    }

    @Test
    public void testIgnoreKillersAtDifferentPly() {

        String fen = "1k6/8/8/6q1/8/5PP1/6BP/6K1 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("g1", "h1"),
                Notation.fromNotation("g1", "f1"),
                Notation.fromNotation("f3", "f4"),
                Notation.fromNotation("h2", "h4")
                ));

        Move killerMove = new Move(21, 29);
        moveOrderer.addKillerMove(2, killerMove);

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertFalse(orderedMoves.get(0).matches(Notation.fromNotation("f3", "f4")));

    }

    @Test
    public void testHistoryHeuristicOrdering() {

        String fen = "5rk1/5ppp/8/8/8/8/4QPPP/5RK1 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("g1", "h1"),
                Notation.fromNotation("f2", "f4"),
                Notation.fromNotation("g2", "g4"),
                Notation.fromNotation("h2", "h4"),
                Notation.fromNotation("f2", "f3"),
                Notation.fromNotation("g2", "g3"),
                Notation.fromNotation("h2", "h3"),
                Notation.fromNotation("f1", "e1"),
                Notation.fromNotation("e2", "e7")
                ));

        moveOrderer.addHistoryScore(Notation.fromNotation("f1", "e1"), new SearchStack(), 5, 0, true);

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(Notation.fromNotation("f1", "e1")));

    }

    @Test
    public void testWinningCaptureBias() {

        String fen = "7k/8/8/8/4r3/3P1Q2/8/7K w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("h1", "h2"),
                Notation.fromNotation("h1", "g1"),
                Notation.fromNotation("h1", "g2"),
//                NotationUtils.fromNotation("f3", "e4", PieceType.QUEEN),
                Notation.fromNotation("d3", "e4")
                ));

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(Notation.fromNotation("d3", "e4")));

    }

    @Test
    public void testWinningCaptureBeatsLosingCapture() {

        String fen = "7k/8/8/5p2/4r3/3P1Q2/8/7K w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("h1", "h2"),
                Notation.fromNotation("h1", "g1"),
                Notation.fromNotation("h1", "g2"),
                Notation.fromNotation("f3", "e4"),
                Notation.fromNotation("d3", "e4")
                ));

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, null, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(Notation.fromNotation("d3", "e4")));
        Assertions.assertTrue(orderedMoves.get(1).matches(Notation.fromNotation("f3", "e4")));

    }

    @Test
    public void testPreviousBestMoveBias() {

        String fen = "2b1r1k1/5pp1/B5Np/8/8/8/3R1PPP/6K1 b - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = new ArrayList<>(List.of(
                Notation.fromNotation("c8", "a6"),
                Notation.fromNotation("g8", "h7"),
                Notation.fromNotation("f7", "f5"),
                Notation.fromNotation("h6", "h5"),
                Notation.fromNotation("e8", "e1")
                ));

        Move previousBestMove = Notation.fromNotation("e8", "e1");

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), moves, previousBestMove, 1);

        Assertions.assertTrue(orderedMoves.get(0).matches(Notation.fromNotation("e8", "e1")));

    }

    // TODO re-write
    @Disabled
    @Test
    public void testComplexPosition() {

        String fen = "1k6/3p2pp/rnb1P3/8/N3Q3/1P5p/P1PPPpN1/1K6 b - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> legalMoves = moveGenerator.generateMoves(board);

        // Previous best move
        Move previousBestMove = new Move(Notation.fromNotation("b8"), Notation.fromNotation("c7"));
        // Add killer move
        moveOrderer.addKillerMove(3, new Move(Notation.fromNotation("d7"), Notation.fromNotation("d5"), Move.PAWN_DOUBLE_MOVE_FLAG));
        // Add history moves
        moveOrderer.addHistoryScore(new Move(Notation.fromNotation("h7"), Notation.fromNotation("h5"), Move.PAWN_DOUBLE_MOVE_FLAG), new SearchStack(), 2, 0, false);
        moveOrderer.addHistoryScore(new Move(Notation.fromNotation("g7"), Notation.fromNotation("g5"), Move.PAWN_DOUBLE_MOVE_FLAG), new SearchStack(), 3, 0, false);
        // Add a white history move just to confirm it is not used
        moveOrderer.addHistoryScore(new Move(Notation.fromNotation("h3"), Notation.fromNotation("h2")), new SearchStack(), 2, 0, true);

        List<Move> orderedMoves = moveOrderer.orderMoves(board, new SearchStack(), legalMoves, previousBestMove, 3);

        Assertions.assertEquals(orderedMoves.get(0), new Move(Notation.fromNotation("b8"), Notation.fromNotation("c7")));
        Assertions.assertEquals(orderedMoves.get(1), new Move(Notation.fromNotation("f2"), Notation.fromNotation("f1"), Move.PROMOTE_TO_QUEEN_FLAG));
        Assertions.assertEquals(orderedMoves.get(2), new Move(Notation.fromNotation("c6"), Notation.fromNotation("e4")));
        Assertions.assertEquals(orderedMoves.get(3), new Move(Notation.fromNotation("h3"), Notation.fromNotation("g2")));
        Assertions.assertEquals(orderedMoves.get(4), new Move(Notation.fromNotation("b6"), Notation.fromNotation("a4")));
        Assertions.assertEquals(orderedMoves.get(5), new Move(Notation.fromNotation("d7"), Notation.fromNotation("e6")));
        Assertions.assertEquals(orderedMoves.get(6), new Move(Notation.fromNotation("d7"), Notation.fromNotation("d5"), Move.PAWN_DOUBLE_MOVE_FLAG));
        Assertions.assertEquals(orderedMoves.get(7), new Move(Notation.fromNotation("c6"), Notation.fromNotation("a4")));
        Assertions.assertEquals(orderedMoves.get(8), new Move(Notation.fromNotation("a6"), Notation.fromNotation("a4")));
        Assertions.assertEquals(orderedMoves.get(9), new Move(Notation.fromNotation("f2"), Notation.fromNotation("f1"), Move.PROMOTE_TO_KNIGHT_FLAG));
        Assertions.assertEquals(orderedMoves.get(10), new Move(Notation.fromNotation("f2"), Notation.fromNotation("f1"), Move.PROMOTE_TO_BISHOP_FLAG));
        Assertions.assertEquals(orderedMoves.get(11), new Move(Notation.fromNotation("f2"), Notation.fromNotation("f1"), Move.PROMOTE_TO_ROOK_FLAG));
        Assertions.assertEquals(orderedMoves.get(12), new Move(Notation.fromNotation("g7"), Notation.fromNotation("g5"), Move.PAWN_DOUBLE_MOVE_FLAG));
        Assertions.assertEquals(orderedMoves.get(13), new Move(Notation.fromNotation("h7"), Notation.fromNotation("h5"), Move.PAWN_DOUBLE_MOVE_FLAG));

    }

}