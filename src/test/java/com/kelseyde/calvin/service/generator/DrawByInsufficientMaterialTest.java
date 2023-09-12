package com.kelseyde.calvin.service.generator;

import com.kelseyde.calvin.model.Board;
import com.kelseyde.calvin.model.Colour;
import com.kelseyde.calvin.model.Piece;
import com.kelseyde.calvin.model.PieceType;
import com.kelseyde.calvin.model.game.*;
import com.kelseyde.calvin.utils.MoveUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DrawByInsufficientMaterialTest {

    @Test
    public void testKingvsKing() {

        Board board = Board.emptyBoard();
        board.setPiece(28, new Piece(Colour.WHITE, PieceType.KING));

        board.setPiece(44, new Piece(Colour.BLACK, PieceType.KING));
        board.setPiece(27, new Piece(Colour.BLACK, PieceType.QUEEN));

        Game game = Game.fromPosition(board);
        ActionResult result = game.executeAction(move("e4", "d4"));

        // king captures queen -> K vs K
        Assertions.assertTrue(result.isDraw());
        Assertions.assertEquals(DrawType.INSUFFICIENT_MATERIAL, result.getDrawType());

    }

    @Test
    public void testKingvsKingBishop() {

        Board board = Board.emptyBoard();
        board.setPiece(28, new Piece(Colour.WHITE, PieceType.KING));
        board.setPiece(25, new Piece(Colour.WHITE, PieceType.BISHOP));

        board.setPiece(44, new Piece(Colour.BLACK, PieceType.KING));
        board.setPiece(43, new Piece(Colour.BLACK, PieceType.QUEEN));

        Game game = Game.fromPosition(board);
        ActionResult result = game.executeAction(move("b4", "d6"));

        // bishop captures queen -> K vs KB
        Assertions.assertTrue(result.isDraw());
        Assertions.assertEquals(DrawType.INSUFFICIENT_MATERIAL, result.getDrawType());

    }

    @Test
    public void testKingvsKingKnight() {

        Board board = Board.emptyBoard();
        board.setPiece(28, new Piece(Colour.WHITE, PieceType.KING));
        board.setPiece(26, new Piece(Colour.WHITE, PieceType.KNIGHT));

        board.setPiece(44, new Piece(Colour.BLACK, PieceType.KING));
        board.setPiece(43, new Piece(Colour.BLACK, PieceType.QUEEN));

        Game game = Game.fromPosition(board);
        ActionResult result = game.executeAction(move("c4", "d6"));

        // knight captures queen -> K vs KN
        Assertions.assertTrue(result.isDraw());
        Assertions.assertEquals(DrawType.INSUFFICIENT_MATERIAL, result.getDrawType());

    }

    @Test
    public void testKingBishopvsKingBishop() {

        Board board = Board.emptyBoard();
        board.setPiece(28, new Piece(Colour.WHITE, PieceType.KING));
        board.setPiece(25, new Piece(Colour.WHITE, PieceType.BISHOP));

        board.setPiece(44, new Piece(Colour.BLACK, PieceType.KING));
        board.setPiece(43, new Piece(Colour.BLACK, PieceType.QUEEN));
        board.setPiece(52, new Piece(Colour.BLACK, PieceType.BISHOP));

        Game game = Game.fromPosition(board);
        ActionResult result = game.executeAction(move("b4", "d6"));

        // bishop captures queen -> KB vs KB
        Assertions.assertTrue(result.isDraw());
        Assertions.assertEquals(DrawType.INSUFFICIENT_MATERIAL, result.getDrawType());

    }

    @Test
    public void testKingKnightvsKingKnight() {

        Board board = Board.emptyBoard();
        board.setPiece(28, new Piece(Colour.WHITE, PieceType.KING));
        board.setPiece(26, new Piece(Colour.WHITE, PieceType.KNIGHT));

        board.setPiece(44, new Piece(Colour.BLACK, PieceType.KING));
        board.setPiece(43, new Piece(Colour.BLACK, PieceType.QUEEN));
        board.setPiece(52, new Piece(Colour.BLACK, PieceType.KNIGHT));

        Game game = Game.fromPosition(board);
        ActionResult result = game.executeAction(move("c4", "d6"));

        // bishop captures queen -> KB vs KB
        Assertions.assertTrue(result.isDraw());
        Assertions.assertEquals(DrawType.INSUFFICIENT_MATERIAL, result.getDrawType());

    }

    private GameAction move(String startSquare, String endSquare) {
        return GameAction.builder()
                .actionType(ActionType.MOVE)
                .move(MoveUtils.fromNotation(startSquare, endSquare))
                .build();
    }

}
