package com.kelseyde.calvin.generation.check;

import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.generation.Attacks;

public class PinCalculator {

    private final RayCalculator rayCalculator = new RayCalculator();

    private long pinMask;
    private long[] pinRayMasks;

    public record PinData(long pinMask, long[] pinRayMasks) {}

    /**
     * Calculates the pin mask and pin ray masks for the given board position.
     *
     * @param board The game board.
     * @param white Whether the current player is white.
     * @return The pin data containing the pin mask and pin ray masks.
     */
    public PinData calculatePinMask(Board board, boolean white) {
        pinMask = 0L;
        pinRayMasks = new long[64];

        int kingSquare = Bitwise.getNextBit(board.king(white));
        long friendlies = board.pieces(white);
        long opponents = board.pieces(!white);

        // Calculate possible orthogonal (queen or rook) pins
        long opponentOrthogonalSliders = board.rooks(!white) | board.queens(!white);
        if (opponentOrthogonalSliders != 0) {
            long possibleOrthogonalPinners = Attacks.rookAttacks(kingSquare, 0) & opponentOrthogonalSliders;
            calculatePins(kingSquare, friendlies, opponents, possibleOrthogonalPinners);
        }

        // Calculate possible diagonal (queen or bishop) pins
        long opponentDiagonalSliders = board.bishops(!white) | board.queens(!white);
        if (opponentDiagonalSliders != 0) {
            long possibleDiagonalPinners = Attacks.bishopAttacks(kingSquare, 0) & opponentDiagonalSliders;
            calculatePins(kingSquare, friendlies, opponents, possibleDiagonalPinners);
        }

        return new PinData(pinMask, pinRayMasks);
    }

    /**
     * Calculates pins between the king and potential pinners.
     *
     * @param kingSquare The square of the king.
     * @param friendlies The bitboard of friendly pieces.
     * @param opponents The bitboard of opponent pieces.
     * @param possiblePinners The bitboard of possible pinners.
     */
    private void calculatePins(int kingSquare, long friendlies, long opponents, long possiblePinners) {
        while (possiblePinners != 0) {
            int possiblePinner = Bitwise.getNextBit(possiblePinners);
            long ray = rayCalculator.rayBetween(kingSquare, possiblePinner);

            // Skip if there are opponents between the king and the possible pinner
            if ((ray & opponents) != 0) {
                possiblePinners = Bitwise.popBit(possiblePinners);
                continue;
            }

            long friendliesBetween = ray & friendlies;
            // If there is exactly one friendly piece between the king and the pinner, it's pinned
            if (Bitwise.countBits(friendliesBetween) == 1) {
                int friendlySquare = Bitwise.getNextBit(friendliesBetween);
                pinMask |= friendliesBetween;
                pinRayMasks[friendlySquare] = ray | (1L << possiblePinner);
            }

            possiblePinners = Bitwise.popBit(possiblePinners);
        }
    }
}
