package com.kelseyde.calvin.board.bitboard;

import com.kelseyde.calvin.utils.BoardUtils;

public class BitboardUtils {

    public static long shiftNorth(long board) {
        return board << 8;
    }

    public static long shiftSouth(long board) {
        return board >>> 8;
    }

    public static long shiftEast(long board) {
        return board << 1 &~ Bits.FILE_A;
    }

    public static long shiftWest(long board) {
        return (board >>> 1) &~ Bits.FILE_H;
    }

    public static long shiftNorthEast(long board) {
        return board << 9 &~ Bits.FILE_A;
    }

    public static long shiftSouthEast(long board) {
        return board >>> 7 &~ Bits.FILE_A;
    }

    public static long shiftNorthWest(long board) {
        return board << 7 &~ Bits.FILE_H;
    }

    public static long shiftSouthWest(long board) {
        return board >>> 9 &~ Bits.FILE_H;
    }

    /**
     * Get the index of the least-significant bit in the bitboard
     */
    public static int getLSB(long board) {
        return Long.numberOfTrailingZeros(board);
    }

    /**
     * Get a bitboard with the least-significant bit removed from the given bitboard.
     */
    public static long popLSB(long board) {
        return board & (board - 1);
    }

    public static void print(long board) {
        String s = Long.toBinaryString(board);
        for (int i = 7; i >= 0; i--) {
            for (int n = 0; n < 8; n++) {
                int index = BoardUtils.squareIndex(i , n);
                boolean isBit = ((board >>> index) & 1) == 1;
                System.out.print(isBit ? 1 : 0);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static long getFileBitboard(int file) {
        return switch (file) {
            case -1 -> 0L;
            case 0 -> Bits.FILE_A;
            case 1 -> Bits.FILE_B;
            case 2 -> Bits.FILE_C;
            case 3 -> Bits.FILE_D;
            case 4 -> Bits.FILE_E;
            case 5 -> Bits.FILE_F;
            case 6 -> Bits.FILE_G;
            case 7 -> Bits.FILE_H;
            default -> throw new IllegalArgumentException("Invalid file " + file);
        };
    }

}
