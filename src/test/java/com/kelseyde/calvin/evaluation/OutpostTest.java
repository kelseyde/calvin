package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.utils.notation.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OutpostTest {

    @Test
    public void testOutpost() {

        String fen = "6k1/p6p/2P1P3/P1p1p3/7P/2p5/8/5K2 w - - 0 1";
        Board board = FEN.toBoard(fen);
        long whitePawns = board.getPawns(true);
        long blackPawns = board.getPawns(false);

        Assertions.assertEquals(0, Bitwise.isOutpost(0, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(1, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(2, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(3, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(4, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(5, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(6, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(7, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(8, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(9, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(10, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(11, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(12, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(13, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(14, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(15, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(16, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(17, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(18, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(19, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(20, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(21, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(22, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(23, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(24, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(25, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(26, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(27, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(28, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(29, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(30, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(31, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(32, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(33, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(34, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(35, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(36, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(37, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(38, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(39, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(40, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(41, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(42, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(43, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(44, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(45, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(46, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(47, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(48, whitePawns, blackPawns, true));
        Assertions.assertEquals(1, Bitwise.isOutpost(49, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(50, whitePawns, blackPawns, true));
        Assertions.assertEquals(2, Bitwise.isOutpost(51, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(52, whitePawns, blackPawns, true));
        Assertions.assertEquals(1, Bitwise.isOutpost(53, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(54, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(55, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(56, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(57, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(58, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(59, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(60, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(61, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(62, whitePawns, blackPawns, true));
        Assertions.assertEquals(0, Bitwise.isOutpost(63, whitePawns, blackPawns, true));


        Assertions.assertEquals(0, Bitwise.isOutpost(0, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(1, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(2, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(3, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(4, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(5, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(6, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(7, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(8, blackPawns, whitePawns, false));
        Assertions.assertEquals(1, Bitwise.isOutpost(9, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(10, blackPawns, whitePawns, false));
        Assertions.assertEquals(1, Bitwise.isOutpost(11, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(12, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(13, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(14, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(15, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(16, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(17, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(18, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(19, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(20, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(21, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(22, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(23, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(24, blackPawns, whitePawns, false));
        Assertions.assertEquals(1, Bitwise.isOutpost(25, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(26, blackPawns, whitePawns, false));
        Assertions.assertEquals(2, Bitwise.isOutpost(27, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(28, blackPawns, whitePawns, false));
        Assertions.assertEquals(1, Bitwise.isOutpost(29, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(30, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(31, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(32, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(33, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(34, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(35, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(36, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(37, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(38, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(39, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(40, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(41, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(42, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(43, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(44, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(45, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(46, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(47, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(48, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(49, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(50, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(51, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(52, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(53, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(54, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(55, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(56, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(57, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(58, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(59, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(60, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(61, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(62, blackPawns, whitePawns, false));
        Assertions.assertEquals(0, Bitwise.isOutpost(63, blackPawns, whitePawns, false));

    }

}
