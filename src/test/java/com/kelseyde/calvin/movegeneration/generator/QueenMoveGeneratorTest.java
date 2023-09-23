package com.kelseyde.calvin.movegeneration.generator;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.move.Move;
import com.kelseyde.calvin.board.piece.Piece;
import com.kelseyde.calvin.board.piece.PieceType;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class QueenMoveGeneratorTest {

    private final QueenMoveGenerator generator = new QueenMoveGenerator();

    private final String queen = Piece.getPieceCode(true, PieceType.QUEEN);

    private Board board;

    @BeforeEach
    public void beforeEach() {
        board = TestUtils.emptyBoard();
    }

    @Test
    public void testEmptyBoard() {

        // bottom corner
        assertLegalSquares(0, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 16, 24, 32, 40, 48, 56,
                9, 18, 27, 36, 45, 54, 63));

        // middle of first rank
        assertLegalSquares(3, Set.of(0, 1, 2, 4, 5, 6, 7, 11, 19, 27, 35, 43, 51, 59,
                10, 17, 24, 12, 21, 30, 39));

        // bottom corner
        assertLegalSquares(7, Set.of(0, 1, 2, 3, 4, 5, 6, 15, 23, 31, 39, 47, 55, 63,
                14, 21, 28, 35, 42, 49, 56));

        // middle square
        assertLegalSquares(28, Set.of(24, 25, 26, 27, 29, 30, 31, 4, 12, 20, 36, 44, 52, 60,
                1, 10, 19, 37, 46, 55, 7, 14, 21, 35, 42, 49, 56));

        // top corner
        assertLegalSquares(56, Set.of(57, 58, 59, 60, 61, 62, 63, 0, 8, 16, 24, 32, 40, 48,
                7, 14, 21, 28, 35, 42, 49));

        // middle of last rank
        assertLegalSquares(60, Set.of(56, 57, 58, 59, 61, 62, 63, 4, 12, 20, 28, 36, 44, 52,
                24, 33, 42, 51, 39, 46, 53));

        // top corner
        assertLegalSquares(63, Set.of(56, 57, 58, 59, 60, 61, 62, 7, 15, 23, 31, 39, 47, 55,
                0, 9, 18, 27, 36, 45, 54));

    }

    @Test
    public void capturingOpponentPiecesEndsVector() {

        int startSquare = 28; //e4

        board.setPiece(10, PieceType.PAWN, false, true);
        board.setPiece(14, PieceType.KNIGHT, false, true);
        board.setPiece(42, PieceType.BISHOP, false, true);
        board.setPiece(46, PieceType.ROOK, false, true);
        board.setPiece(12, PieceType.PAWN, false, true);
        board.setPiece(26, PieceType.KNIGHT, false, true);
        board.setPiece(30, PieceType.BISHOP, false, true);
        board.setPiece(44, PieceType.ROOK, false, true);

        assertLegalSquares(startSquare, Set.of(19, 21, 35, 37, 10, 14, 42, 46, 12, 20, 26, 27, 29, 30, 36, 44));

    }

    @Test
    public void reachingSameColourPiecesEndsVector() {

        int startSquare = 28; //e4

        board.setPiece(10, PieceType.PAWN, true, true);
        board.setPiece(14, PieceType.KNIGHT, true, true);
        board.setPiece(42, PieceType.BISHOP, true, true);
        board.setPiece(46, PieceType.ROOK, true, true);
        board.setPiece(12, PieceType.PAWN, true, true);
        board.setPiece(26, PieceType.KNIGHT, true, true);
        board.setPiece(30, PieceType.BISHOP, true, true);
        board.setPiece(44, PieceType.ROOK, true, true);

        assertLegalSquares(startSquare, Set.of(19, 21, 35, 37, 27, 29, 20, 36));

    }

    private void assertLegalSquares(int startSquare, Set<Integer> expectedLegalSquares) {
        board.setPiece(startSquare, PieceType.QUEEN, true, true);
        Set<Integer> legalSquares = generator.generatePseudoLegalMoves(board).stream()
                .filter(move -> move.getStartSquare() == startSquare)
                .map(Move::getEndSquare)
                .collect(Collectors.toSet());
        Assertions.assertEquals(expectedLegalSquares, legalSquares);
        board = TestUtils.emptyBoard();
    }

}