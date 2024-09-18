package com.kelseyde.calvin.generation.drawcalculator;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.search.Score;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DrawByInsufficientMaterialTest {

    @Test
    public void testKingVersusKing() {

        String fen = "8/8/4k3/8/3qK3/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "e4", "d4"));

        // king captures queen -> K vs K
        Assertions.assertTrue(Score.isInsufficientMaterial(board));

    }

    @Test
    public void testKingVersusKingBishop() {

        String fen = "8/8/3qk3/8/1B2K3/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "b4", "d6"));

        // bishop captures queen -> K vs KB
        Assertions.assertTrue(Score.isInsufficientMaterial(board));

    }

    @Test
    public void testKingVersusKingKnight() {

        String fen = "8/8/3qk3/8/2N1K3/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "c4", "d6"));

        // knight captures queen -> K vs KN
        Assertions.assertTrue(Score.isInsufficientMaterial(board));

    }

    @Test
    public void testKingBishopVersusKingBishop() {

        String fen = "8/5b2/3qk3/8/1B2K3/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "b4", "d6"));

        // bishop captures queen -> KB vs KB
        Assertions.assertTrue(Score.isInsufficientMaterial(board));

    }

    @Test
    public void testKingKnightKnightVersusKingKnightIsNotInsufficientMaterial() {

        String fen = "8/5nn1/3qk3/8/2N1K3/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        Assertions.assertFalse(Score.isEffectiveDraw(board));

        board.makeMove(TestUtils.getLegalMove(board, "c4", "d6"));

        // knight captures queen -> KNN vs KN
        Assertions.assertFalse(Score.isInsufficientMaterial(board));

    }

}
