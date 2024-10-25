package com.kelseyde.calvin.movegen;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.utils.notation.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PinCalculatorTest {

    private final MoveGenerator movegen = new MoveGenerator();

    @Test
    public void testNoSlidersAndNoPins() {

        String fen = "4nk2/5p2/8/8/8/2PP4/2K1N3/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testDiagonalSlidersButNoPins() {

        String fen = "4qkb1/5pb1/8/8/8/2PP4/2KBB3/2Q5 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testOrthogonalSlidersButNoPins() {

        String fen = "4qkr1/5pr1/3r4/8/8/2PP2R1/2KRR3/2Q5 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testOrthogonalCheckIsNotAPin() {

        String fen = "8/8/3rk3/8/8/3K4/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testDiagonalCheckIsNotAPin() {

        String fen = "8/8/4k3/5b2/8/3K4/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testCannotOrthogonallyPinThroughSameSidePiece() {

        String fen = "5k2/4q3/4p3/8/8/8/8/4K3 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());
    }

    @Test
    public void testSimplePositiveFileOrthogonalPin() {

        String fen = "4k3/4r3/8/8/8/8/4P3/4K3 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 12, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0L, movegen.getPinMask());

    }

    @Test
    public void testSimpleNegativeFileOrthogonalPin() {

        String fen = "4k3/4b3/8/8/8/8/4R3/4K3 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0L, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 52, movegen.getPinMask());

    }

    @Test
    public void testFileOrthogonalTwoWayPin() {

        String fen = "8/3k4/3q4/8/8/3R4/3K4/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 19, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 43, movegen.getPinMask());

    }

    @Test
    public void testFileOrthogonalDoubleFriendlyPinEqualsNoPin() {

        String fen = "8/3k4/3q4/3q4/3R4/3R4/3K4/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testFileOrthogonalDoubleOpponentPinIsStillPin() {

        String fen = "4k3/4q3/4r3/8/4N3/4K3/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 28, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testSimplePositiveRankOrthogonalPin() {

        String fen = "KR4nk/8/8/8/8/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 62, movegen.getPinMask());

    }

    @Test
    public void testSimpleNegativeRankOrthogonalPin() {

        String fen = "KN4qk/8/8/8/8/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 57, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testSimplePositiveDiagonalPin() {

        String fen = "K1n5/2q5/nqr1B3/5r2/6k1/8/8/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 37, movegen.getPinMask());

    }

    @Test
    public void testSimpleNegativeDiagonalPin() {

        String fen = "8/8/8/8/1k6/2q5/3QK3/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 18, movegen.getPinMask());

    }

    @Test
    public void testDiagonalDoubleFriendlyPinEqualsNoPin() {

        String fen = "7k/8/8/1b6/2N5/3R4/3QK3/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testSimplePositiveAntiDiagonalPin() {

        String fen = "7k/6b1/8/8/8/8/1B6/1KRRRRR1 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 54, movegen.getPinMask());

    }

    @Test
    public void testSimpleNegativeAntiDiagonalPin() {

        String fen = "5q2/4Q3/3K4/7r/7r/7r/7r/7k w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 52, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testTwoWayAntiDiagonalPin() {

        String fen = "7K/8/8/4B3/3b4/8/8/k7 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 36, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(1L << 27, movegen.getPinMask());

    }

    @Test
    public void testAntiDiagonalDoubleFriendlyPinEqualsNoPin() {

        String fen = "7K/8/8/4B3/3b4/2b5/8/k7 w - - 0 1";
        Board board = FEN.toBoard(fen);

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 36, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testBothSidesOrthogonalRankPin() {

        String fen = "3r4/8/8/5k2/8/8/8/r1NKN2r w - - 0 1";
        Board board = FEN.toBoard(fen);

        long expectedPinMask = 0L;
        expectedPinMask |= 1L << 2;
        expectedPinMask |= 1L << 4;

        movegen.calculatePins(board, true);
        Assertions.assertEquals(expectedPinMask, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(0, movegen.getPinMask());

    }

    @Test
    public void testBothSidesOrthogonalFilePin() {

        String fen = "K7/8/4R3/4p3/4k3/4p3/4Q3/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        long expectedPinMask = 0L;
        expectedPinMask |= 1L << 20;
        expectedPinMask |= 1L << 36;

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(expectedPinMask, movegen.getPinMask());

    }

    @Test
    public void testBothSidesAntiDiagonalPin() {

        String fen = "K7/8/6B1/5p2/4k3/3p4/2B5/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        long expectedPinMask = 0L;
        expectedPinMask |= 1L << 19;
        expectedPinMask |= 1L << 37;

        movegen.calculatePins(board, true);
        Assertions.assertEquals(0, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(expectedPinMask, movegen.getPinMask());

    }

    @Test
    public void testBothSidesDiagonalPin() {

        String fen = "K7/8/2B5/3q1p2/4k3/3p1r2/6B1/8 w - - 0 1";
        Board board = FEN.toBoard(fen);

        long expectedPinMask = 0L;
        expectedPinMask |= 1L << 35;
        expectedPinMask |= 1L << 21;

        movegen.calculatePins(board, true);
        Assertions.assertEquals(1L << 42, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(expectedPinMask, movegen.getPinMask());
    }

    @Test
    public void testCrazyPinsEverywhere() {

        String fen = "1b1R1q2/2PnP3/rNNK4/4NR2/4pbp1/5k2/4bnp1/3B1R1R w - - 0 1";
        Board board = FEN.toBoard(fen);

        long expectedWhitePinMask = 0L;
        expectedWhitePinMask |= 1L << 50;
        expectedWhitePinMask |= 1L << 52;
        expectedWhitePinMask |= 1L << 36;

        long expectedBlackPinMask = 0L;
        expectedBlackPinMask |= 1L << 29;
        expectedBlackPinMask |= 1L << 12;
        expectedBlackPinMask |= 1L << 13;

        movegen.calculatePins(board, true);
        Assertions.assertEquals(expectedWhitePinMask, movegen.getPinMask());

        movegen.calculatePins(board, false);
        Assertions.assertEquals(expectedBlackPinMask, movegen.getPinMask());

    }

}