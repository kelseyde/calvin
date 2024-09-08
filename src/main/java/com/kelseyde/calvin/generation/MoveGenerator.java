package com.kelseyde.calvin.generation;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.check.PinCalculator;
import com.kelseyde.calvin.generation.check.PinCalculator.PinData;
import com.kelseyde.calvin.generation.check.RayCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all the legal moves in a given position.
 * Using a hybrid of pseudo-legal and legal move generation: first we calculate the bitboards for checking pieces and
 * pinned pieces. If there is a check, we filter out all moves that do not resolve the check. Finally, we filter out all
 * moves that leave the king in (a new) check.
 */
public class MoveGenerator implements MoveGeneration {

    private final PinCalculator pinCalculator = new PinCalculator();
    private final RayCalculator rayCalculator = new RayCalculator();

    private int checkersCount;
    private long checkersMask;
    private long pinMask;
    private long[] pinRayMasks;
    private long captureMask;
    private long pushMask;
    private MoveFilter filter;
    private boolean white;

    private long pawns;
    private long knights;
    private long bishops;
    private long rooks;
    private long queens;
    private long king;

    private List<Move> legalMoves;

    /**
     * Generates all legal moves for the current board position.
     *
     * @param board The current board state.
     * @return A list of all legal moves.
     */
    @Override
    public List<Move> generateMoves(Board board) {
        return generateMoves(board, MoveFilter.ALL);
    }

    /**
     * Generates legal moves for the current board position based on the provided filter.
     *
     * @param board  The current board state.
     * @param filter The filter to apply to move generation.
     * @return A list of legal moves filtered according to the given criteria.
     */
    @Override
    public List<Move> generateMoves(Board board, MoveFilter filter) {

        white = board.isWhiteToMove();

        // Initialise piece fields
        initPieces(board, white);

        int kingSquare = Bitwise.getNextBit(king);
        this.filter = filter;

        // Initialize capture and push masks
        captureMask = Bits.ALL_SQUARES;
        pushMask = Bits.ALL_SQUARES;

        // Calculate pins and checks
        PinData pinData = pinCalculator.calculatePinMask(board, white);
        pinMask = pinData.pinMask();
        pinRayMasks = pinData.pinRayMasks();
        checkersMask = calculateAttackerMask(board, 1L << kingSquare);
        checkersCount = Bitwise.countBits(checkersMask);

        int estimatedLegalMoves = estimateLegalMoves();
        legalMoves = new ArrayList<>(estimatedLegalMoves);

        if (checkersCount > 0 && filter == MoveFilter.QUIET) {
            return legalMoves;
        }

        // Generate king moves first
        generateKingMoves(board);

        if (checkersCount == 2) {
            // If we are in double-check, the only legal moves are king moves
            return legalMoves;
        }

        if (checkersCount == 1) {
            // If only one checker, we can evade check by capturing it
            captureMask = checkersMask;

            int checkerSquare = Bitwise.getNextBit(checkersMask);
            if (board.pieceAt(checkerSquare).isSlider()) {
                // If the piece giving check is a slider, we can evade check by blocking it
                pushMask = rayCalculator.rayBetween(checkerSquare, kingSquare);
            } else {
                // If the piece is not a slider, we can only evade check by capturing it
                // Therefore all non-capture 'push' moves are illegal.
                pushMask = Bits.NO_SQUARES;
            }
        }

        // Generate all the other legal moves using the capture and push masks
        generatePawnMoves(board);
        generateKnightMoves(board);
        generateAllSlidingMoves(board);
        generateCastlingMoves(board);

        return legalMoves;

    }

    /**
     * Checks if the specified side is in check.
     *
     * @param board   The current board state.
     * @param white Indicates whether the side in question is white.
     * @return True if the specified side is in check, otherwise false.
     */
    @Override
    public boolean isCheck(Board board, boolean white) {
        long king = board.king(white);
        return isAttacked(board, white, king);
    }

    private void generatePawnMoves(Board board) {

        if (pawns == 0) return;
        long opponents = board.pieces(!white);
        long occupied = board.getOccupied();
        int opponentKing = Bitwise.getNextBit(board.king(!white));

        long filterMask = checkersCount > 0 ? Bits.ALL_SQUARES :
        switch (filter) {
            case ALL -> Bits.ALL_SQUARES;
            case CAPTURES_ONLY -> opponents;
            case NOISY -> opponents | Attacks.pawnAttacks(1L << opponentKing, !white);
            case QUIET -> ~opponents & ~Attacks.pawnAttacks(1L << opponentKing, !white);
        };
        if (filterMask == Bits.NO_SQUARES) {
            return;
        }

        if (filter != MoveFilter.CAPTURES_ONLY) {

            long singleMoves = Bitwise.pawnSingleMoves(pawns, occupied, white) & pushMask & filterMask;
            long doubleMoves = Bitwise.pawnDoubleMoves(pawns, occupied, white) & pushMask & filterMask;

            while (singleMoves != 0) {
                int endSquare = Bitwise.getNextBit(singleMoves);
                int startSquare = white ? endSquare - 8 : endSquare + 8;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare));
                }
                singleMoves = Bitwise.popBit(singleMoves);
            }
            while (doubleMoves != 0) {
                int endSquare = Bitwise.getNextBit(doubleMoves);
                int startSquare = white ? endSquare - 16 : endSquare + 16;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare, Move.PAWN_DOUBLE_MOVE_FLAG));
                }
                doubleMoves = Bitwise.popBit(doubleMoves);
            }
        }

        if (filter != MoveFilter.QUIET) {
            long leftCaptures = Bitwise.pawnLeftCaptures(pawns, opponents, white) & captureMask & filterMask;
            long rightCaptures = Bitwise.pawnRightCaptures(pawns, opponents, white) & captureMask & filterMask;
            long pushPromotions = Bitwise.pawnPushPromotions(pawns, occupied, white) & pushMask;
            long leftCapturePromotions = Bitwise.pawnLeftCapturePromotions(pawns, opponents, white) & (captureMask | pushMask);
            long rightCapturePromotions = Bitwise.pawnRightCapturePromotions(pawns, opponents, white) & (captureMask | pushMask);

            while (leftCaptures != 0) {
                int endSquare = Bitwise.getNextBit(leftCaptures);
                int startSquare = white ? endSquare - 7 : endSquare + 9;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare));
                }
                leftCaptures = Bitwise.popBit(leftCaptures);
            }
            while (rightCaptures != 0) {
                int endSquare = Bitwise.getNextBit(rightCaptures);
                int startSquare = white ? endSquare - 9 : endSquare + 7;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.add(new Move(startSquare, endSquare));
                }
                rightCaptures = Bitwise.popBit(rightCaptures);
            }

            if (board.getGameState().getEnPassantFile() >= 0) {
                long enPassantFile = Bitwise.getFileBitboard(board.getGameState().getEnPassantFile());
                long leftEnPassants = Bitwise.pawnLeftEnPassants(pawns, enPassantFile, white);
                long rightEnPassants = Bitwise.pawnRightEnPassants(pawns, enPassantFile, white);
                while (leftEnPassants != 0) {
                    int endSquare = Bitwise.getNextBit(leftEnPassants);
                    int startSquare = white ? endSquare - 7 : endSquare + 9;
                    // En passant is complicated; just test legality by making the move on the board and checking
                    // whether the king is attacked.
                    Move move = new Move(startSquare, endSquare, Move.EN_PASSANT_FLAG);
                    if (!leavesKingInCheck(board, move, white)) {
                        legalMoves.add(move);
                    }
                    leftEnPassants = Bitwise.popBit(leftEnPassants);
                }
                while (rightEnPassants != 0) {
                    int endSquare = Bitwise.getNextBit(rightEnPassants);
                    int startSquare = white ? endSquare - 9 : endSquare + 7;
                    // En passant is complicated; just test legality by making the move on the board and checking
                    // whether the king is attacked.
                    Move move = new Move(startSquare, endSquare, Move.EN_PASSANT_FLAG);
                    if (!leavesKingInCheck(board, move, white)) {
                        legalMoves.add(move);
                    }
                    rightEnPassants = Bitwise.popBit(rightEnPassants);
                }
            }

            while (pushPromotions != 0) {
                int endSquare = Bitwise.getNextBit(pushPromotions);
                int startSquare = white ? endSquare - 8 : endSquare + 8;
                if (!isPinned(startSquare)) {
                    legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
                }
                pushPromotions = Bitwise.popBit(pushPromotions);
            }
            while (leftCapturePromotions != 0) {
                int endSquare = Bitwise.getNextBit(leftCapturePromotions);
                int startSquare = white ? endSquare - 7 : endSquare + 9;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
                }
                leftCapturePromotions = Bitwise.popBit(leftCapturePromotions);
            }
            while (rightCapturePromotions != 0) {
                int endSquare = Bitwise.getNextBit(rightCapturePromotions);
                int startSquare = white ? endSquare - 9 : endSquare + 7;
                if (!isPinned(startSquare) || isMovingAlongPinRay(startSquare, endSquare)) {
                    legalMoves.addAll(getPromotionMoves(startSquare, endSquare));
                }
                rightCapturePromotions = Bitwise.popBit(rightCapturePromotions);
            }
        }

    }

    private void generateKnightMoves(Board board) {
        if (knights == 0) return;
        long opponents = board.pieces(!white);
        int opponentKing = Bitwise.getNextBit(board.king(!white));

        // Initialize filter mask based on move filter type
        long filterMask = checkersCount > 0 ? Bits.ALL_SQUARES :
        switch (filter) {
            case ALL -> Bits.ALL_SQUARES;
            case CAPTURES_ONLY -> opponents;
            case NOISY -> opponents | Attacks.knightAttacks(opponentKing);
            case QUIET -> ~opponents & ~Attacks.knightAttacks(opponentKing);
        };
        if (filterMask == Bits.NO_SQUARES) {
            return;
        }

        // Exclude pinned knights from generating moves
        long unpinnedKnights = knights & ~pinMask;

        // Generate legal knight moves
        while (unpinnedKnights != 0) {
            int startSquare = Bitwise.getNextBit(unpinnedKnights);
            long possibleMoves = getKnightAttacks(board, startSquare, white) & (pushMask | captureMask) & filterMask;
            while (possibleMoves != 0) {
                int endSquare = Bitwise.getNextBit(possibleMoves);
                legalMoves.add(new Move(startSquare, endSquare));
                possibleMoves = Bitwise.popBit(possibleMoves);
            }
            unpinnedKnights = Bitwise.popBit(unpinnedKnights);
        }
    }

    private void generateKingMoves(Board board) {
        int startSquare = Bitwise.getNextBit(king);
        long friendlies = board.pieces(white);
        long opponents = board.pieces(!white);

        long filterMask = checkersCount > 0 ? Bits.ALL_SQUARES :
        switch (filter) {
            case ALL -> Bits.ALL_SQUARES;
            case CAPTURES_ONLY, NOISY -> opponents;
            case QUIET -> ~opponents;
        };
        if (filterMask == Bits.NO_SQUARES) {
            return;
        }

        long kingMoves = Attacks.kingAttacks(startSquare) & ~friendlies & filterMask;

        // Temporarily remove the king from the board
        board.removeKing(white);

        // Generate legal king moves
        while (kingMoves != 0) {
            int endSquare = Bitwise.getNextBit(kingMoves);
            // Check if the end square is not attacked by the opponent
            if (!isAttacked(board, white, 1L << endSquare)) {
                legalMoves.add(new Move(startSquare, endSquare));
            }
            kingMoves = Bitwise.popBit(kingMoves);
        }

        // Restore the king to its original position on the board
        board.addKing(startSquare, white);
    }

    private void generateCastlingMoves(Board board) {
        if ((filter != MoveFilter.ALL && filter != MoveFilter.QUIET)
                || checkersMask != 0) {
            return;
        }
        int startSquare = Bitwise.getNextBit(king);
        long occupied = board.getOccupied();

        boolean isKingsideAllowed = board.getGameState().isKingsideCastlingAllowed(white);
        if (isKingsideAllowed) {
            generateCastlingMove(board, white, true, startSquare, occupied);
        }

        boolean isQueensideAllowed = board.getGameState().isQueensideCastlingAllowed(white);
        if (isQueensideAllowed) {
            generateCastlingMove(board, white, false, startSquare, occupied);
        }

    }

    private void generateCastlingMove(Board board, boolean white, boolean isKingside, int startSquare, long occupied) {
        long travelSquares = getCastleTravelSquares(white, isKingside);
        long blockedSquares = travelSquares & occupied;
        long safeSquares = getCastleSafeSquares(white, isKingside);
        if (blockedSquares == 0 && !isAttacked(board, white, safeSquares)) {
            int endSquare = getCastleEndSquare(white, isKingside);
            legalMoves.add(new Move(startSquare, endSquare, Move.CASTLE_FLAG));
        }
    }

    private void generateAllSlidingMoves(Board board) {
        if (filter == MoveFilter.ALL) {
            long diagonalSliders = bishops | queens;
            long orthogonalSliders = rooks | queens;
            generateSlidingMoves(board, diagonalSliders, false, true);
            generateSlidingMoves(board, orthogonalSliders, true, false);
        } else {
            generateSlidingMoves(board, bishops, false, true);
            generateSlidingMoves(board, rooks, true, false);
            generateSlidingMoves(board, queens, true, true);
        }
    }

    private void generateSlidingMoves(Board board, long sliders, boolean isOrthogonal, boolean isDiagonal) {
        long opponents = board.pieces(!white);
        long occupied = board.getOccupied();
        long friendlies = board.pieces(white);

        while (sliders != 0) {
            int startSquare = Bitwise.getNextBit(sliders);
            long attackMask = getSlidingAttacks(startSquare, friendlies, occupied, isDiagonal, isOrthogonal);

            attackMask &= pushMask | captureMask;

            // Apply move filters
            long filterMask = checkersCount > 0 ? Bits.ALL_SQUARES :
            switch (filter) {
                case ALL -> Bits.ALL_SQUARES;
                case CAPTURES_ONLY -> opponents;
                case NOISY -> getCaptureAndCheckMask(board, white, opponents, occupied, isDiagonal, isOrthogonal);
                case QUIET -> ~getCaptureAndCheckMask(board, white, opponents, occupied, isDiagonal, isOrthogonal);
            };
            if (filterMask == Bits.NO_SQUARES) {
                return;
            }
            attackMask &= filterMask;

            // Handle pinned pieces
            if (isPinned(startSquare)) {
                attackMask &= pinRayMasks[startSquare];
            }

            sliders = Bitwise.popBit(sliders);
            while (attackMask != 0) {
                int endSquare = Bitwise.getNextBit(attackMask);
                legalMoves.add(new Move(startSquare, endSquare));
                attackMask = Bitwise.popBit(attackMask);
            }
        }
    }

    private long getCaptureAndCheckMask(Board board, boolean white, long opponents, long occupied, boolean isDiagonal, boolean isOrthogonal) {
        int opponentKing = Bitwise.getNextBit(board.king(!white));
        long filterMask = opponents;
        if (isDiagonal) {
            filterMask |= Attacks.bishopAttacks(opponentKing, occupied);
        }
        if (isOrthogonal) {
            filterMask |= Attacks.rookAttacks(opponentKing, occupied);
        }
        return filterMask;
    }

    public long getPawnAttacks(Board board, int square, boolean white) {
        long attackMask = 0L;
        long squareBB = 1L << square;
        long friendlies = board.pieces(white);

        long leftCapture = white ?
                Bitwise.shiftNorthWest(squareBB) &~ friendlies &~ Bits.FILE_H :
                Bitwise.shiftSouthWest(squareBB) &~ friendlies &~ Bits.FILE_H;
        attackMask |= leftCapture;

        long rightCapture = white ?
                Bitwise.shiftNorthEast(squareBB) &~ friendlies &~ Bits.FILE_A :
                Bitwise.shiftSouthEast(squareBB) &~ friendlies &~ Bits.FILE_A;
        attackMask |= rightCapture;

        return attackMask;
    }

    public long getKnightAttacks(Board board, int square, boolean white) {
        long friendlies = board.pieces(white);
        return Attacks.knightAttacks(square) &~ friendlies;
    }

    public long getBishopAttacks(Board board, int square, boolean white) {
        long occupied = board.getOccupied();
        long friendlies = board.pieces(white);
        return getSlidingAttacks(square, friendlies, occupied, true, false);
    }

    public long getRookAttacks(Board board, int square, boolean white) {
        long occupied = board.getOccupied();
        long friendlies = board.pieces(white);
        return getSlidingAttacks(square, friendlies, occupied, false, true);
    }

    public long getQueenAttacks(Board board, int square, boolean white) {
        long occupied = board.getOccupied();
        long friendlies = board.pieces(white);
        return getSlidingAttacks(square, friendlies, occupied, true, true);
    }

    public long getKingAttacks(Board board, int square, boolean white) {
        long friendlies = board.pieces(white);
        return Attacks.kingAttacks(square) &~ friendlies;
    }

    private long getSlidingAttacks(int square, long friendlies, long occ, boolean isDiagonal, boolean isOrthogonal) {
        long attackMask = 0L;
        if (isOrthogonal) {
            attackMask |= Attacks.rookAttacks(square, occ);
        }
        if (isDiagonal) {
            attackMask |= Attacks.bishopAttacks(square, occ);
        }
        return attackMask &~ friendlies;
    }

    private long calculateAttackerMask(Board board, long squareMask) {
        long attackerMask = 0L;
        while (squareMask != 0) {
            int square = Bitwise.getNextBit(squareMask);

            long opponentPawns = board.pawns(!white);
            long pawnAttackMask = getPawnAttacks(board, square, white);
            attackerMask |= pawnAttackMask & opponentPawns;

            long opponentKnights = board.knights(!white);
            long knightAttackMask = getKnightAttacks(board, square, white);
            attackerMask |= knightAttackMask & opponentKnights;

            long opponentBishops = board.bishops(!white);
            long bishopAttackMask = getBishopAttacks(board, square, white);
            attackerMask |= bishopAttackMask & opponentBishops;

            long opponentRooks = board.rooks(!white);
            long rookAttackMask = getRookAttacks(board, square, white);
            attackerMask |= rookAttackMask & opponentRooks;

            long opponentQueens = board.queens(!white);
            attackerMask |= (bishopAttackMask | rookAttackMask) & opponentQueens;

            long opponentKing = board.king(!white);
            long kingAttackMask = getKingAttacks(board, square, white);
            attackerMask |= kingAttackMask & opponentKing;

            squareMask = Bitwise.popBit(squareMask);
        }
        return attackerMask;
    }

    private boolean isAttacked(Board board, boolean white, long squareMask) {
        while (squareMask != 0) {
            int square = Bitwise.getNextBit(squareMask);

            long opponentPawns = board.pawns(!white);
            if (opponentPawns != 0) {
                long pawnAttackMask = getPawnAttacks(board, square, white);
                if ((pawnAttackMask & opponentPawns) != 0) {
                    return true;
                }
            }

            long opponentKnights = board.knights(!white);
            if (opponentKnights != 0) {
                long knightAttackMask = getKnightAttacks(board, square, white);
                if ((knightAttackMask & opponentKnights) != 0) {
                    return true;
                }
            }

            long opponentBishops = board.bishops(!white);
            long opponentQueens = board.queens(!white);
            long diagonalSliders = opponentBishops | opponentQueens;
            if (diagonalSliders != 0) {
                long bishopAttackMask = getBishopAttacks(board, square, white);
                if ((bishopAttackMask & diagonalSliders) != 0) {
                    return true;
                }
            }

            long opponentRooks = board.rooks(!white);
            long orthogonalSliders = opponentRooks | opponentQueens;
            if (orthogonalSliders != 0) {
                long rookAttackMask = getRookAttacks(board, square, white);
                if ((rookAttackMask & orthogonalSliders) != 0) {
                    return true;
                }
            }

            long opponentKing = board.king(!white);
            long kingAttackMask = getKingAttacks(board, square, white);
            if ((kingAttackMask & opponentKing) != 0) {
                return true;
            }

            squareMask = Bitwise.popBit(squareMask);
        }
        return false;
    }

    private List<Move> getPromotionMoves(int startSquare, int endSquare) {
        return List.of(new Move(startSquare, endSquare, Move.PROMOTE_TO_QUEEN_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_ROOK_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_BISHOP_FLAG),
                        new Move(startSquare, endSquare, Move.PROMOTE_TO_KNIGHT_FLAG));
    }


    private boolean leavesKingInCheck(Board board, Move move, boolean white) {
        board.makeMove(move);
        int kingSquare = white ? Bitwise.getNextBit(board.king(true)) : Bitwise.getNextBit(board.king(false));
        boolean isAttacked = isAttacked(board, white, 1L << kingSquare);
        board.unmakeMove();
        return isAttacked;
    }

    private boolean isPinned(int startSquare) {
        return (1L << startSquare & pinMask) != 0;
    }

    private boolean isMovingAlongPinRay(int startSquare, int endSquare) {
        long pinRay = pinRayMasks[startSquare];
        return (1L << endSquare & pinRay) != 0;
    }

    private long getCastleTravelSquares(boolean white, boolean isKingside) {
        if (isKingside) return white ? Bits.WHITE_KINGSIDE_CASTLE_TRAVEL_MASK : Bits.BLACK_KINGSIDE_CASTLE_TRAVEL_MASK;
        else return white ? Bits.WHITE_QUEENSIDE_CASTLE_TRAVEL_MASK : Bits.BLACK_QUEENSIDE_CASTLE_TRAVEL_MASK;
    }

    private long getCastleSafeSquares(boolean white, boolean isKingside) {
        if (isKingside) return white ? Bits.WHITE_KINGSIDE_CASTLE_SAFE_MASK : Bits.BLACK_KINGSIDE_CASTLE_SAFE_MASK;
        else return white ? Bits.WHITE_QUEENSIDE_CASTLE_SAFE_MASK : Bits.BLACK_QUEENSIDE_CASTLE_SAFE_MASK;
    }

    private int getCastleEndSquare(boolean white, boolean isKingside) {
        if (isKingside) return white ? 6 : 62;
        else return white ? 2 : 58;
    }

    /**
     * Estimate the number of legal moves in the current position, based on the piece count and
     * the average number of legal moves per piece. Used to initialise the legal moves ArrayList
     * with a 'best guess', to reduce the number of times the ArrayList has to grow during move
     * generation, yielding a small increase in performance.
     */
    private int estimateLegalMoves() {
        return (Bitwise.countBits(pawns) * 2) +
                (Bitwise.countBits(knights) * 3) +
                (Bitwise.countBits(bishops) * 3) +
                (Bitwise.countBits(rooks) * 6) +
                (Bitwise.countBits(queens) * 9) +
                (Bitwise.countBits(king) * 3);
    }

    private void initPieces(Board board, boolean white) {
        pawns = board.pawns(white);
        knights = board.knights(white);
        bishops = board.bishops(white);
        rooks = board.rooks(white);
        queens = board.queens(white);
        king = board.king(white);
    }

}
