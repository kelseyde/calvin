package com.kelseyde.calvin;

import com.kelseyde.calvin.board.Game;
import com.kelseyde.calvin.board.result.GameResult;
import com.kelseyde.calvin.utils.NotationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CheckTest {

    @Test
    public void checkBlocksOtherMoves() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));
        game.makeMove(NotationUtils.fromNotation("d8", "h4"));

        // check
        game.makeMove(NotationUtils.fromNotation("h5", "f7"));

        // try to ignore check with other moves
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("a7", "a6")));

    }

    @Test
    public void cannotMovePinnedPawn() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));

        // black tries to move pinned f-pawn
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("f7", "f6")));

    }

    @Test
    public void cannotEnPassantWithPinnedPawn() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("d2", "d4"));
        game.makeMove(NotationUtils.fromNotation("e5", "d4"));
        game.makeMove(NotationUtils.fromNotation("e4", "e5"));
        game.makeMove(NotationUtils.fromNotation("d8", "e7"));
        game.makeMove(NotationUtils.fromNotation("g1", "f3"));
        game.makeMove(NotationUtils.fromNotation("d7", "d5"));

        // black tries to en-passant with pinned e-pawn
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e5", "d6")));

    }

    @Test
    public void cannotMovePinnedKnight() {
        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("f7", "f5"));
        game.makeMove(NotationUtils.fromNotation("e4", "f5"));
        game.makeMove(NotationUtils.fromNotation("e7", "e6"));
        game.makeMove(NotationUtils.fromNotation("d2", "d4"));
        game.makeMove(NotationUtils.fromNotation("e6", "f5"));
        game.makeMove(NotationUtils.fromNotation("d1", "e2"));
        // block check with knight
        game.makeMove(NotationUtils.fromNotation("g8", "e7"));
        game.makeMove(NotationUtils.fromNotation("a2", "a4"));
        //try moving pinned knight
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e7", "g8")));
    }

    @Test
    public void cannotMovePinnedBishop() {
        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("f7", "f5"));
        game.makeMove(NotationUtils.fromNotation("e4", "f5"));
        game.makeMove(NotationUtils.fromNotation("e7", "e6"));
        game.makeMove(NotationUtils.fromNotation("d2", "d4"));
        game.makeMove(NotationUtils.fromNotation("e6", "f5"));
        game.makeMove(NotationUtils.fromNotation("d1", "e2"));
        // block check with bishop
        game.makeMove(NotationUtils.fromNotation("f8", "e7"));
        game.makeMove(NotationUtils.fromNotation("a2", "a4"));
        //try moving pinned bishop
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e7", "f8")));
    }

    @Test
    public void cannotMovePinnedQueen() {
        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("f7", "f5"));
        game.makeMove(NotationUtils.fromNotation("e4", "f5"));
        game.makeMove(NotationUtils.fromNotation("e7", "e6"));
        game.makeMove(NotationUtils.fromNotation("d2", "d4"));
        game.makeMove(NotationUtils.fromNotation("e6", "f5"));
        game.makeMove(NotationUtils.fromNotation("d1", "e2"));
        // block check with queen
        game.makeMove(NotationUtils.fromNotation("d8", "e7"));
        game.makeMove(NotationUtils.fromNotation("a2", "a4"));
        //try moving pinned queen
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e7", "d8")));
    }

    @Test
    public void cannotMoveFromCheckIntoAnotherCheck() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));
        game.makeMove(NotationUtils.fromNotation("d8", "h4"));

        // check
        game.makeMove(NotationUtils.fromNotation("h5", "f7"));

        // try to move to another checked square
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e8", "e7")));

    }

    @Test
    public void canCaptureUnprotectedCheckingPiece() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));
        game.makeMove(NotationUtils.fromNotation("d8", "h4"));

        // check
        game.makeMove(NotationUtils.fromNotation("h5", "f7"));
        // capture checking queen
        game.makeMove(NotationUtils.fromNotation("e8", "f7"));

    }

    @Test
    public void cannotCaptureProtectedCheckingPieceWithKing() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("f1", "c4"));
        game.makeMove(NotationUtils.fromNotation("b8", "c6"));
        // wayward queen!
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));
        game.makeMove(NotationUtils.fromNotation("d8", "e7"));

        // check
        game.makeMove(NotationUtils.fromNotation("h5", "f7"));
        // try capturing checking queen with king
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e8", "f7")));

    }

    @Test
    public void canCaptureProtectedCheckingPieceWithOtherPiece() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("f1", "c4"));
        game.makeMove(NotationUtils.fromNotation("b8", "c6"));
        // wayward queen!
        game.makeMove(NotationUtils.fromNotation("d1", "h5"));
        game.makeMove(NotationUtils.fromNotation("d8", "e7"));

        // check
        game.makeMove(NotationUtils.fromNotation("h5", "f7"));
        // try capturing checking queen with queen
        game.makeMove(NotationUtils.fromNotation("e7", "f7"));

    }

    @Test
    public void cannotCastleOutOfCheck() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e6"));
        game.makeMove(NotationUtils.fromNotation("d2", "d4"));
        game.makeMove(NotationUtils.fromNotation("d7", "d5"));
        game.makeMove(NotationUtils.fromNotation("e4", "d5"));
        game.makeMove(NotationUtils.fromNotation("e6", "d5"));
        game.makeMove(NotationUtils.fromNotation("g1", "f3"));
        game.makeMove(NotationUtils.fromNotation("g8", "f6"));
        game.makeMove(NotationUtils.fromNotation("f1", "d3"));
        game.makeMove(NotationUtils.fromNotation("f8", "d6"));

        // check
        game.makeMove(NotationUtils.fromNotation("d1", "e2"));

        // try to castle out of check
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e8", "g8")));

    }

    @Test
    public void cannotCastleThroughCheck() {

        Game game = new Game();
        game.makeMove(NotationUtils.fromNotation("e2", "e4"));
        game.makeMove(NotationUtils.fromNotation("e7", "e5"));
        game.makeMove(NotationUtils.fromNotation("f2", "f4"));
        game.makeMove(NotationUtils.fromNotation("b7", "b6"));
        game.makeMove(NotationUtils.fromNotation("f1", "a6"));
        game.makeMove(NotationUtils.fromNotation("c8", "a6"));
        game.makeMove(NotationUtils.fromNotation("g1", "f3"));
        game.makeMove(NotationUtils.fromNotation("d7", "d6"));

        // try to castle through the bishop check
        assertIllegalMove(game.makeMove(NotationUtils.fromNotation("e1", "g1")));

    }

    private void assertIllegalMove(GameResult result) {
        Assertions.assertEquals(GameResult.ResultType.ILLEGAL_MOVE, result.getResultType());
    }

}
