package com.kelseyde.calvin.board;

import com.kelseyde.calvin.movegen.Attacks;

import java.util.List;
import java.util.Map;

public class Bits {

    public static int next(final long board) {
        return Long.numberOfTrailingZeros(board);
    }

    public static long pop(long board) {
        return board & (board - 1);
    }

    public static int count(long board) {
        return Long.bitCount(board);
    }

    public static long north(long board) {
        return board << 8;
    }

    public static long south(long board) {
        return board >>> 8;
    }

    public static long east(long board) {
        return (board << 1) & ~File.A;
    }

    public static long west(long board) {
        return (board >>> 1) & ~File.H;
    }

    public static long northEast(long board) {
        return (board << 9) & ~File.A;
    }

    public static long southEast(long board) {
        return (board >>> 7) & ~File.A;
    }

    public static long northWest(long board) {
        return (board << 7) & ~File.H;
    }

    public static long southWest(long board) {
        return (board >>> 9) & ~File.H;
    }

    public static class Square {

        public static final int COUNT = 64;
        public static final long ALL = ~0L;
        public static final long NONE = 0L;

        public static int of(int rank, int file) {
            return (rank << 3) + file;
        }

        public static boolean isValid(int sq) {
            return sq >= 0 && sq < Square.COUNT;
        }

        public static String toNotation(int sq) {
            return File.toFileNotation(sq) + Rank.toRankNotation(sq);
        }

        public static int fromNotation(String algebraic) {
            int xOffset = List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').indexOf(algebraic.charAt(0));
            int yAxis = (Integer.parseInt(Character.valueOf(algebraic.charAt(1)).toString()) - 1) * 8;
            return yAxis + xOffset;
        }

    }

    public static class File {

        public static final long A = 0b0000000100000001000000010000000100000001000000010000000100000001L;
        public static final long B = 0b0000001000000010000000100000001000000010000000100000001000000010L;
        public static final long C = 0b0000010000000100000001000000010000000100000001000000010000000100L;
        public static final long D = 0b0000100000001000000010000000100000001000000010000000100000001000L;
        public static final long E = 0b0001000000010000000100000001000000010000000100000001000000010000L;
        public static final long F = 0b0010000000100000001000000010000000100000001000000010000000100000L;
        public static final long G = 0b0100000001000000010000000100000001000000010000000100000001000000L;
        public static final long H = 0b1000000010000000100000001000000010000000100000001000000010000000L;

        public static final Map<Integer, String> FILE_CHAR_MAP = Map.of(
                0, "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h"
        );

        public static int of(int sq) {
            return sq & 7;
        }

        public static long toBitboard(int file) {
            return 0x0101010101010101L << file;
        }

        public static String toFileNotation(int sq) {
            return FILE_CHAR_MAP.get(of(sq));
        }
    }

    public static class Rank {

        public static final long FIRST  = 0b0000000000000000000000000000000000000000000000000000000011111111L;
        public static final long SECOND = 0b0000000000000000000000000000000000000000000000001111111100000000L;
        public static final long THIRD  = 0b0000000000000000000000000000000000000000111111110000000000000000L;
        public static final long FOURTH = 0b0000000000000000000000000000000011111111000000000000000000000000L;
        public static final long FIFTH  = 0b0000000000000000000000001111111100000000000000000000000000000000L;
        public static final long SIXTH  = 0b0000000000000000111111110000000000000000000000000000000000000000L;
        public static final long SEVENTH = 0b0000000011111111000000000000000000000000000000000000000000000000L;
        public static final long EIGHTH  = 0b1111111100000000000000000000000000000000000000000000000000000000L;

        public static final Map<Integer, String> RANK_CHAR_MAP = Map.of(
                0, "1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8"
        );

        public static int of(int sq) {
            return sq >>> 3;
        }

        public static String toRankNotation(int sq) {
            return RANK_CHAR_MAP.get(of(sq));
        }
    }

    public static class Ray {

        /**
         * Calculates the ray (bitboard) between two squares on the chessboard.
         */
        public static long between(int from, int to) {
            if (!Square.isValid(from) || !Square.isValid(to) || (from == to)) {
                return 0L;
            }
            int offset = direction(from, to);
            if (offset == 0) return 0L;
            long ray = 0L;
            int sq = from + offset;
            while (Square.isValid(sq) && sq != to) {
                ray |= 1L << sq;
                sq += offset;
            }
            return ray;
        }

        /**
         * Determines the direction offset between two squares on the chessboard.
         */
        private static int direction(int from, int to) {
            int startRank = Rank.of(from);
            int endRank = Rank.of(to);
            int startFile = File.of(from);
            int endFile = File.of(to);
            if (startRank == endRank) {
                return from > to ? -1 : 1;
            }
            else if (startFile == endFile) {
                return from > to ? -8 : 8;
            }
            else if (Math.abs(startRank - endRank) == Math.abs(startFile - endFile)) {
                return from > to ? (from - to) % 9 == 0 ? -9 : -7 : (to - from) % 9 == 0 ? 9 : 7;
            }
            else if (startRank + startFile == endRank + endFile) {
                return from > to ? -9 : 9;
            }
            return 0;
        }

    }

    public static class Pin {

        public static class PinData {
            public long pinMask;
            public long[] pinRayMasks;
            public PinData() {}
        }

        private static final long[] pinRayMasks = new long[Square.COUNT];
        private static final PinData pinData = new PinData();

        /**
         * Calculates the pin mask and pin ray masks for the given board position.
         *
         * @param board The game board.
         * @param white Whether the current player is white.
         * @return The pin data containing the pin mask and pin ray masks.
         */
        public static PinData calculatePins(Board board, boolean white) {
            long pinMask = 0L;

            int kingSquare = Bits.next(board.getKing(white));
            long friendlies = board.getPieces(white);
            long opponents = board.getPieces(!white);

            long possiblePinners = 0L;

            // Calculate possible orthogonal pins
            long orthogonalSliders = board.getRooks(!white) | board.getQueens(!white);
            if (orthogonalSliders != 0) {
                possiblePinners |= Attacks.rookAttacks(kingSquare, 0) & orthogonalSliders;
            }

            // Calculate possible diagonal pins
            long diagonalSliders = board.getBishops(!white) | board.getQueens(!white);
            if (diagonalSliders != 0) {
                possiblePinners |= Attacks.bishopAttacks(kingSquare, 0) & diagonalSliders;
            }

            while (possiblePinners != 0) {
                int possiblePinner = Bits.next(possiblePinners);
                long ray = Ray.between(kingSquare, possiblePinner);

                // Skip if there are opponents between the king and the possible pinner
                if ((ray & opponents) != 0) {
                    possiblePinners = Bits.pop(possiblePinners);
                    continue;
                }

                long friendliesBetween = ray & friendlies;
                // If there is exactly one friendly piece between the king and the pinner, it's pinned
                if (Bits.count(friendliesBetween) == 1) {
                    int friendlySquare = Bits.next(friendliesBetween);
                    pinMask |= friendliesBetween;
                    pinRayMasks[friendlySquare] = ray | (1L << possiblePinner);
                }

                possiblePinners = Bits.pop(possiblePinners);
            }

            pinData.pinMask = pinMask;
            pinData.pinRayMasks = pinRayMasks;
            return pinData;
        }

    }

    public static class Castling {
        // Masks for the squares that must be unoccupied for legal castling
        public static final long WHITE_QUEENSIDE_TRAVEL_MASK = 0x000000000000000EL;
        public static final long WHITE_KINGSIDE_TRAVEL_MASK = 0x0000000000000060L;
        public static final long BLACK_QUEENSIDE_TRAVEL_MASK = WHITE_QUEENSIDE_TRAVEL_MASK << (7 * 8);
        public static final long BLACK_KINGSIDE_TRAVEL_MASK = WHITE_KINGSIDE_TRAVEL_MASK  << (7 * 8);

        // Masks for the squares that must not be attacked for legal castling
        public static final long WHITE_QUEENSIDE_SAFE_MASK = 0x000000000000001CL;
        public static final long WHITE_KINGSIDE_SAFE_MASK = WHITE_QUEENSIDE_SAFE_MASK << 2;
        public static final long BLACK_QUEENSIDE_SAFE_MASK = WHITE_QUEENSIDE_SAFE_MASK << (7 * 8);
        public static final long BLACK_KINGSIDE_SAFE_MASK = WHITE_KINGSIDE_SAFE_MASK  << (7 * 8);

        public static final int INITIAL_CASTLING_RIGHTS = 0b1111;
        public static final int CLEAR_WHITE_CASTLING_MASK = 0b1100;
        public static final int CLEAR_BLACK_CASTLING_MASK = 0b0011;
        public static final int CLEAR_WHITE_KINGSIDE_MASK = 0b1110;
        public static final int CLEAR_BLACK_KINGSIDE_MASK = 0b1011;
        public static final int CLEAR_WHITE_QUEENSIDE_MASK = 0b1101;
        public static final int CLEAR_BLACK_QUEENSIDE_MASK = 0b0111;
    }


}
