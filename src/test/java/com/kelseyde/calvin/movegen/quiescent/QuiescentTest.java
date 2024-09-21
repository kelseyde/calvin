package com.kelseyde.calvin.movegen.quiescent;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class QuiescentTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    @Test
    public void testGenerateKnightMoves() {

        // Includes a pinned knight and a friendly blocker
        String fen = "1k1r1N1n/pppb3p/P2b4/4N3/1N6/6K1/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY);
        List<Move> expected = List.of(
                Notation.fromNotation("a6", "b7"),
                Notation.fromNotation("f8", "d7"),
                Notation.fromNotation("f8", "h7")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY);
        expected = List.of(
                Notation.fromNotation("a6", "b7"),
                Notation.fromNotation("f8", "d7"),
                Notation.fromNotation("f8", "h7"),
                Notation.fromNotation("b4", "c6")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testGenerateBishopMoves() {

        String fen = "6k1/4bpp1/7p/8/p6b/1B4B1/2B2K2/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY);
        List<Move> expected = List.of(
                Notation.fromNotation("g3", "h4"),
                Notation.fromNotation("b3", "f7"),
                Notation.fromNotation("b3", "a4")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY);
        expected = List.of(
                Notation.fromNotation("g3", "h4"),
                Notation.fromNotation("b3", "f7"),
                Notation.fromNotation("b3", "a4"),
                Notation.fromNotation("c2", "h7")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testGenerateQueenMoves() {

        String fen = "8/8/1KQr4/2Q5/6p1/6pk/2Q4N/4r3 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY);
        List<Move> expected = List.of(
                Notation.fromNotation("h2", "g4"),
                Notation.fromNotation("c6", "d6"),
                Notation.fromNotation("c5", "d6")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY);
        expected = List.of(
                Notation.fromNotation("h2", "g4"),
                Notation.fromNotation("c6", "d6"),
                Notation.fromNotation("c5", "d6"),
                Notation.fromNotation("c2", "g2"),
                Notation.fromNotation("c2", "h7"),
                Notation.fromNotation("c5", "h5")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testPawnAndPromotionMoves() {

        String fen = "3k4/8/8/8/6pP/1p2p3/3B1p2/2K3R1 b - h3 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY);
        List<Move> expected = List.of(
                Notation.fromNotation("g4", "h3"),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_QUEEN_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_ROOK_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_BISHOP_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_QUEEN_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_ROOK_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_BISHOP_FLAG),
                Notation.fromNotation("e3", "d2")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY);
        expected = List.of(
                Notation.fromNotation("g4", "h3"),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_QUEEN_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_ROOK_FLAG),
                Notation.fromNotation("f2", "g1", Move.PROMOTE_TO_BISHOP_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_QUEEN_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_ROOK_FLAG),
                Notation.fromNotation("f2", "f1", Move.PROMOTE_TO_BISHOP_FLAG),
                Notation.fromNotation("e3", "d2"),
                Notation.fromNotation("b3", "b2")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testRookMoves() {

        String fen = "1bR2N1b/R3nk2/5p2/8/8/8/2K3R1/4R2R w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY);
        List<Move> expected = List.of(
                Notation.fromNotation("h1", "h8"),
                Notation.fromNotation("e1", "e7"),
                Notation.fromNotation("a7", "e7"),
                Notation.fromNotation("c8", "b8")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY);
        expected = List.of(
                Notation.fromNotation("h1", "h8"),
                Notation.fromNotation("e1", "e7"),
                Notation.fromNotation("a7", "e7"),
                Notation.fromNotation("c8", "b8"),
                Notation.fromNotation("h1", "h7"),
                Notation.fromNotation("g2", "g7")
        );
        assertMoves(expected, moves);

    }

    private void assertMoves(List<Move> expected, List<Move> actual) {
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertTrue(expected.stream().allMatch(move -> actual.stream().anyMatch(move::matches)));
    }

}
