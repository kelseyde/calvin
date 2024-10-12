package com.kelseyde.calvin.movegen.quiescent;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.utils.notation.FEN;
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

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY).toList();
        List<Move> expected = List.of(
                Move.fromUCI("a6b7"),
                Move.fromUCI("f8d7"),
                Move.fromUCI("f8h7")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY).toList();
        expected = List.of(
                Move.fromUCI("a6b7"),
                Move.fromUCI("f8d7"),
                Move.fromUCI("f8h7"),
                Move.fromUCI("b4c6")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testGenerateBishopMoves() {

        String fen = "6k1/4bpp1/7p/8/p6b/1B4B1/2B2K2/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY).toList();
        List<Move> expected = List.of(
                Move.fromUCI("g3h4"),
                Move.fromUCI("b3f7"),
                Move.fromUCI("b3a4")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY).toList();
        expected = List.of(
                Move.fromUCI("g3h4"),
                Move.fromUCI("b3f7"),
                Move.fromUCI("b3a4"),
                Move.fromUCI("c2h7")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testGenerateQueenMoves() {

        String fen = "8/8/1KQr4/2Q5/6p1/6pk/2Q4N/4r3 w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY).toList();
        List<Move> expected = List.of(
                Move.fromUCI("h2g4"),
                Move.fromUCI("c6d6"),
                Move.fromUCI("c5d6")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY).toList();
        expected = List.of(
                Move.fromUCI("h2g4"),
                Move.fromUCI("c6d6"),
                Move.fromUCI("c5d6"),
                Move.fromUCI("c2g2"),
                Move.fromUCI("c2h7"),
                Move.fromUCI("c5h5")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testPawnAndPromotionMoves() {

        String fen = "3k4/8/8/8/6pP/1p2p3/3B1p2/2K3R1 b - h3 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY).toList();
        List<Move> expected = List.of(
                Move.fromUCI("g4h3"),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_QUEEN_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_ROOK_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_BISHOP_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_QUEEN_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_ROOK_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_BISHOP_FLAG),
                Move.fromUCI("e3d2")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY).toList();
        expected = List.of(
                Move.fromUCI("g4h3"),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_QUEEN_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_ROOK_FLAG),
                Move.fromUCI("f2g1", Move.PROMOTE_TO_BISHOP_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_QUEEN_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_KNIGHT_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_ROOK_FLAG),
                Move.fromUCI("f2f1", Move.PROMOTE_TO_BISHOP_FLAG),
                Move.fromUCI("e3d2"),
                Move.fromUCI("b3b2")
        );
        assertMoves(expected, moves);

    }

    @Test
    public void testRookMoves() {

        String fen = "1bR2N1b/R3nk2/5p2/8/8/8/2K3R1/4R2R w - - 0 1";
        Board board = FEN.toBoard(fen);

        List<Move> moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.CAPTURES_ONLY).toList();
        List<Move> expected = List.of(
                Move.fromUCI("h1h8"),
                Move.fromUCI("e1e7"),
                Move.fromUCI("a7e7"),
                Move.fromUCI("c8b8")
        );
        assertMoves(expected, moves);

        moves = moveGenerator.generateMoves(board, MoveGenerator.MoveFilter.NOISY).toList();
        expected = List.of(
                Move.fromUCI("h1h8"),
                Move.fromUCI("e1e7"),
                Move.fromUCI("a7e7"),
                Move.fromUCI("c8b8"),
                Move.fromUCI("h1h7"),
                Move.fromUCI("g2g7")
        );
        assertMoves(expected, moves);

    }

    private void assertMoves(List<Move> expected, List<Move> actual) {
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertTrue(expected.stream().allMatch(move -> actual.stream().anyMatch(move::matches)));
    }

}
