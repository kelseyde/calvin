package com.kelseyde.calvin.movegen;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bits.*;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all the legal moves in a given position.
 * Using a hybrid of pseudo-legal and legal move generation: first we calculate the bitboards for checking pieces and
 * pinned pieces. If there is a check, we filter out all moves that do not resolve the check. Finally, we filter out all
 * moves that leave the king in (a new) check.
 */
public class MoveGenerator {

    private int checkersCount;
    private long checkersMask;
    private long pinMask;
    private final long[] pinRayMasks = new long[64];
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

    public List<Move> generateMoves(Board board) {
        return generateMoves(board, MoveFilter.ALL);
    }

    public List<Move> generateMoves(Board board, MoveFilter filter) {

        white = board.isWhite();

        // Initialise piece fields
        initPieces(board, white);

        final int kingSquare = Bits.next(king);
        this.filter = filter;

        // Initialize capture and push masks
        captureMask = Square.ALL;
        pushMask = Square.ALL;

        // Calculate pins and checks
        calculatePins(board, white);
        checkersMask = calculateCheckers(board, kingSquare);
        checkersCount = Bits.count(checkersMask);

        final int estimatedLegalMoves = estimateLegalMoves();
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

            final int checkerSquare = Bits.next(checkersMask);
            if (board.pieceAt(checkerSquare).isSlider()) {
                // If the piece giving check is a slider, we can evade check by blocking it
                pushMask = Ray.between(checkerSquare, kingSquare);
            } else {
                // If the piece is not a slider, we can only evade check by capturing it
                // Therefore all non-capture 'push' moves are illegal.
                pushMask = Square.NONE;
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
    public boolean isCheck(Board board, boolean white) {
        final long king = board.getKing(white);
        return isAttacked(board, white, king);
    }

    private void generatePawnMoves(Board board) {
        if (pawns == 0) return;

        final long opponents = board.getPieces(!white);
        final long occupied = board.getOccupied();
        final int opponentKing = Bits.next(board.getKing(!white));

        // Precompute attack and filter masks
        final long opponentAttackMask = Attacks.pawnAttacks(Bits.of(opponentKing), !white);
        final long filterMask = getFilterMask(opponents, opponentAttackMask);

        if (filterMask == Square.NONE) return;

        // Single and double pawn pushes
        if (filter != MoveFilter.CAPTURES_ONLY) {
            generatePawnPushes(occupied, filterMask);
        }

        // Pawn captures, en passant, and promotions
        if (filter != MoveFilter.QUIET) {
            generatePawnCaptures(opponents, filterMask);
            generatePromotions(opponents, occupied);
            generateEnPassant(board);
        }
    }

    private void generatePawnPushes(long occupied, long filterMask) {
        // Single and double pawn pushes combined
        long singleMoves = Attacks.pawnSingleMoves(pawns, occupied, white) & pushMask & filterMask;
        long doubleMoves = Attacks.pawnDoubleMoves(pawns, occupied, white) & pushMask & filterMask;

        while (singleMoves != 0) {
            final int to = Bits.next(singleMoves);
            final int from = white ? to - 8 : to + 8;
            if (!isPinned(from) || isMovingAlongPinRay(from, to)) {
                legalMoves.add(new Move(from, to));
            }
            singleMoves = Bits.pop(singleMoves);
        }

        while (doubleMoves != 0) {
            final int to = Bits.next(doubleMoves);
            final int from = white ? to - 16 : to + 16;
            if (!isPinned(from) || isMovingAlongPinRay(from, to)) {
                legalMoves.add(new Move(from, to, Move.PAWN_DOUBLE_MOVE_FLAG));
            }
            doubleMoves = Bits.pop(doubleMoves);
        }
    }

    private void generatePawnCaptures(long opponents, long filterMask) {
        long leftCaptures = Attacks.pawnLeftCaptures(pawns, opponents, white) & captureMask & filterMask;
        long rightCaptures = Attacks.pawnRightCaptures(pawns, opponents, white) & captureMask & filterMask;

        while (leftCaptures != 0) {
            final int to = Bits.next(leftCaptures);
            final int from = white ? to - 7 : to + 9;
            if (!isPinned(from) || isMovingAlongPinRay(from, to)) {
                legalMoves.add(new Move(from, to));
            }
            leftCaptures = Bits.pop(leftCaptures);
        }

        while (rightCaptures != 0) {
            final int to = Bits.next(rightCaptures);
            final int from = white ? to - 9 : to + 7;
            if (!isPinned(from) || isMovingAlongPinRay(from, to)) {
                legalMoves.add(new Move(from, to));
            }
            rightCaptures = Bits.pop(rightCaptures);
        }
    }

    private void generatePromotions(long opponents, long occupied) {
        final long pushPromotions = Attacks.pawnPushPromotions(pawns, occupied, white) & pushMask;
        final long leftCapturePromotions = Attacks.pawnLeftCapturePromotions(pawns, opponents, white) & (captureMask | pushMask);
        final long rightCapturePromotions = Attacks.pawnRightCapturePromotions(pawns, opponents, white) & (captureMask | pushMask);

        generatePromotionMoves(pushPromotions, 8, 8);
        generatePromotionMoves(leftCapturePromotions, 7, 9);
        generatePromotionMoves(rightCapturePromotions, 9, 7);
    }

    private void generatePromotionMoves(long promotionMask, int offsetWhite, int offsetBlack) {
        while (promotionMask != 0) {
            final int to = Bits.next(promotionMask);
            final int from = white ? to - offsetWhite : to + offsetBlack;
            if (!isPinned(from) || isMovingAlongPinRay(from, to)) {
                legalMoves.addAll(getPromotionMoves(from, to));
            }
            promotionMask = Bits.pop(promotionMask);
        }
    }

    private void generateEnPassant(Board board) {
        if (board.getState().getEnPassantFile() < 0) return;

        final long enPassantFile = File.toBitboard(board.getState().getEnPassantFile());
        final long leftEnPassants = Attacks.pawnLeftEnPassants(pawns, enPassantFile, white);
        final long rightEnPassants = Attacks.pawnRightEnPassants(pawns, enPassantFile, white);

        generateEnPassantMoves(board, leftEnPassants, 7, 9);
        generateEnPassantMoves(board, rightEnPassants, 9, 7);
    }

    private void generateEnPassantMoves(Board board, long enPassantMask, int offsetWhite, int offsetBlack) {
        while (enPassantMask != 0) {
            final int to = Bits.next(enPassantMask);
            final int from = white ? to - offsetWhite : to + offsetBlack;
            final Move move = new Move(from, to, Move.EN_PASSANT_FLAG);
            if (!leavesKingInCheck(board, move, white)) {
                legalMoves.add(move);
            }
            enPassantMask = Bits.pop(enPassantMask);
        }
    }

    private long getFilterMask(long opponents, long opponentAttackMask) {
        return checkersCount > 0 ? captureMask | pushMask : switch (filter) {
            case ALL -> Square.ALL;
            case CAPTURES_ONLY -> opponents;
            case NOISY -> opponents | opponentAttackMask;
            case QUIET -> ~opponents & ~opponentAttackMask;
        };
    }

    private void generateKnightMoves(Board board) {
        if (knights == 0) return;
        final long opponents = board.getPieces(!white);
        final int opponentKing = Bits.next(board.getKing(!white));

        // Initialize filter mask based on move filter type
        final long filterMask = checkersCount > 0 ? captureMask | pushMask : switch (filter) {
            case ALL -> Square.ALL;
            case CAPTURES_ONLY -> opponents;
            case NOISY -> opponents | Attacks.knightAttacks(opponentKing);
            case QUIET -> ~opponents & ~Attacks.knightAttacks(opponentKing);
        };
        if (filterMask == Square.NONE) {
            return;
        }

        // Exclude pinned knights from generating moves
        long unpinnedKnights = knights & ~pinMask;

        // Generate legal knight moves
        while (unpinnedKnights != 0) {
            final int from = Bits.next(unpinnedKnights);
            long possibleMoves = getKnightAttacks(board, from, white) & (pushMask | captureMask) & filterMask;
            while (possibleMoves != 0) {
                final int to = Bits.next(possibleMoves);
                legalMoves.add(new Move(from, to));
                possibleMoves = Bits.pop(possibleMoves);
            }
            unpinnedKnights = Bits.pop(unpinnedKnights);
        }
    }

    private void generateKingMoves(Board board) {
        final int from = Bits.next(king);
        final long friendlies = board.getPieces(white);
        final long opponents = board.getPieces(!white);

        final long filterMask = checkersCount > 0 ? captureMask | pushMask : switch (filter) {
            case ALL -> Square.ALL;
            case CAPTURES_ONLY, NOISY -> opponents;
            case QUIET -> ~opponents;
        };
        if (filterMask == Square.NONE) {
            return;
        }

        long kingMoves = Attacks.kingAttacks(from) & ~friendlies & filterMask;

        // Temporarily remove the king from the board
        board.removeKing(white);

        // Generate legal king moves
        while (kingMoves != 0) {
            final int to = Bits.next(kingMoves);
            // Check if the end square is not attacked by the opponent
            if (!isAttacked(board, white, Bits.of(to))) {
                legalMoves.add(new Move(from, to));
            }
            kingMoves = Bits.pop(kingMoves);
        }

        // Restore the king to its original position on the board
        board.addKing(from, white);
    }

    private void generateCastlingMoves(Board board) {
        if ((filter != MoveFilter.ALL && filter != MoveFilter.QUIET)
                || checkersMask != 0) {
            return;
        }
        final int from = Bits.next(king);
        final long occupied = board.getOccupied();

        final boolean isKingsideAllowed = board.getState().isKingsideCastlingAllowed(white);
        if (isKingsideAllowed) {
            generateCastlingMove(board, white, true, from, occupied);
        }

        final boolean isQueensideAllowed = board.getState().isQueensideCastlingAllowed(white);
        if (isQueensideAllowed) {
            generateCastlingMove(board, white, false, from, occupied);
        }

    }

    private void generateCastlingMove(Board board, boolean white, boolean isKingside, int from, long occupied) {
        final long travelSquares = getCastleTravelSquares(white, isKingside);
        final long blockedSquares = travelSquares & occupied;
        final long safeSquares = getCastleSafeSquares(white, isKingside);
        if (blockedSquares == 0 && !isAttacked(board, white, safeSquares)) {
            int to = getCastleEndSquare(white, isKingside);
            legalMoves.add(new Move(from, to, Move.CASTLE_FLAG));
        }
    }

    private void generateAllSlidingMoves(Board board) {
        if (filter == MoveFilter.ALL) {
            final long diagonalSliders = bishops | queens;
            final long orthogonalSliders = rooks | queens;
            generateSlidingMoves(board, diagonalSliders, false, true);
            generateSlidingMoves(board, orthogonalSliders, true, false);
        } else {
            generateSlidingMoves(board, bishops, false, true);
            generateSlidingMoves(board, rooks, true, false);
            generateSlidingMoves(board, queens, true, true);
        }
    }

    private void generateSlidingMoves(Board board, long sliders, boolean isOrthogonal, boolean isDiagonal) {
        final long opponents = board.getPieces(!white);
        final long occupied = board.getOccupied();
        final long friendlies = board.getPieces(white);

        // Apply move filters
        final long filterMask = checkersCount > 0 ? captureMask | pushMask : switch (filter) {
            case ALL -> Square.ALL;
            case CAPTURES_ONLY -> opponents;
            case NOISY -> getCaptureAndCheckMask(board, white, opponents, occupied, isDiagonal, isOrthogonal);
            case QUIET -> ~getCaptureAndCheckMask(board, white, opponents, occupied, isDiagonal, isOrthogonal);
        };
        if (filterMask == Square.NONE) {
            return;
        }

        while (sliders != 0) {
            final int from = Bits.next(sliders);
            long attackMask = getSlidingAttacks(from, friendlies, occupied, isDiagonal, isOrthogonal);

            attackMask &= pushMask | captureMask;
            attackMask &= filterMask;

            // Handle pinned pieces
            if (isPinned(from)) {
                attackMask &= pinRayMasks[from];
            }

            sliders = Bits.pop(sliders);
            while (attackMask != 0) {
                final int to = Bits.next(attackMask);
                legalMoves.add(new Move(from, to));
                attackMask = Bits.pop(attackMask);
            }
        }
    }

    private long getCaptureAndCheckMask(Board board, boolean white, long opponents, long occupied, boolean isDiagonal, boolean isOrthogonal) {
        final int opponentKing = Bits.next(board.getKing(!white));
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
        final long squareBB = Bits.of(square);
        final long friendlies = board.getPieces(white);

        long leftCapture = white ?
                Bits.northWest(squareBB) &~ friendlies &~ File.H :
                Bits.southWest(squareBB) &~ friendlies &~ File.H;
        attackMask |= leftCapture;

        long rightCapture = white ?
                Bits.northEast(squareBB) &~ friendlies &~ File.A :
                Bits.southEast(squareBB) &~ friendlies &~ File.A;
        attackMask |= rightCapture;

        return attackMask;
    }

    public long getKnightAttacks(Board board, int square, boolean white) {
        final long friendlies = board.getPieces(white);
        return Attacks.knightAttacks(square) &~ friendlies;
    }

    public long getBishopAttacks(Board board, int square, boolean white) {
        final long occupied = board.getOccupied();
        final long friendlies = board.getPieces(white);
        return getSlidingAttacks(square, friendlies, occupied, true, false);
    }

    public long getRookAttacks(Board board, int square, boolean white) {
        final long occupied = board.getOccupied();
        final long friendlies = board.getPieces(white);
        return getSlidingAttacks(square, friendlies, occupied, false, true);
    }

    public long getQueenAttacks(Board board, int square, boolean white) {
        final long occupied = board.getOccupied();
        final long friendlies = board.getPieces(white);
        return getSlidingAttacks(square, friendlies, occupied, true, true);
    }

    public long getKingAttacks(Board board, int square, boolean white) {
        final long friendlies = board.getPieces(white);
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

    private long calculateCheckers(Board board, int square) {
        final long occupied = board.getOccupied();
        final long friendlies = board.getPieces(white);
        long attackerMask = 0L;

        final long opponentPawns = board.getPawns(!white);
        if (opponentPawns != 0) {
            final long pawnAttackMask = getPawnAttacks(board, square, white);
            attackerMask |= pawnAttackMask & opponentPawns;
        }

        final long opponentKnights = board.getKnights(!white);
        if (opponentKnights != 0) {
            final long knightAttackMask = getKnightAttacks(board, square, white);
            attackerMask |= knightAttackMask & opponentKnights;
        }

        final long opponentBishops = board.getBishops(!white);
        final long opponentQueens = board.getQueens(!white);
        final long diagonalSliders = opponentBishops | opponentQueens;
        if (diagonalSliders != 0) {
            attackerMask |= getSlidingAttacks(square, friendlies, occupied, true, false) & diagonalSliders;
        }

        final long opponentRooks = board.getRooks(!white);
        final long orthogonalSliders = opponentRooks | opponentQueens;
        if (orthogonalSliders != 0) {
            attackerMask |= getSlidingAttacks(square, friendlies, occupied, false, true) & orthogonalSliders;
        }

        // King can never give check

        return attackerMask;
    }

    private boolean isAttacked(Board board, boolean white, long squareMask) {

        final long opponentPawns = board.getPawns(!white);
        if (opponentPawns != 0) {
            long pawnAttackMask = Attacks.pawnAttacks(squareMask, white);
            if ((pawnAttackMask & opponentPawns) != 0) {
                return true;
            }
        }

        final long occupied = board.getOccupied();

        while (squareMask != 0) {
            final int square = Bits.next(squareMask);

            final long opponentKnights = board.getKnights(!white);
            if (opponentKnights != 0) {
                if ((Attacks.knightAttacks(square) & opponentKnights) != 0) {
                    return true;
                }
            }

            final long opponentBishops = board.getBishops(!white);
            final long opponentQueens = board.getQueens(!white);
            final long diagonalSliders = opponentBishops | opponentQueens;
            if (diagonalSliders != 0) {
                if ((Attacks.bishopAttacks(square, occupied) & diagonalSliders) != 0) {
                    return true;
                }
            }

            final long opponentRooks = board.getRooks(!white);
            final long orthogonalSliders = opponentRooks | opponentQueens;
            if (orthogonalSliders != 0) {
                if ((Attacks.rookAttacks(square, occupied) & orthogonalSliders) != 0) {
                    return true;
                }
            }

            final long opponentKing = board.getKing(!white);
            if ((Attacks.kingAttacks(square) & opponentKing) != 0) {
                return true;
            }

            squareMask = Bits.pop(squareMask);
        }
        return false;
    }

    public void calculatePins(Board board, boolean white) {
        this.pinMask = 0L;

        final int kingSquare = Bits.next(board.getKing(white));
        final long friendlies = board.getPieces(white);
        final long opponents = board.getPieces(!white);

        long possiblePinners = 0L;

        // Calculate possible orthogonal pins
        final long orthogonalSliders = board.getRooks(!white) | board.getQueens(!white);
        if (orthogonalSliders != 0) {
            possiblePinners |= Attacks.rookAttacks(kingSquare, 0) & orthogonalSliders;
        }

        // Calculate possible diagonal pins
        final long diagonalSliders = board.getBishops(!white) | board.getQueens(!white);
        if (diagonalSliders != 0) {
            possiblePinners |= Attacks.bishopAttacks(kingSquare, 0) & diagonalSliders;
        }

        while (possiblePinners != 0) {
            final int possiblePinner = Bits.next(possiblePinners);
            final long ray = Ray.between(kingSquare, possiblePinner);

            // Skip if there are opponents between the king and the possible pinner
            if ((ray & opponents) != 0) {
                possiblePinners = Bits.pop(possiblePinners);
                continue;
            }

            final long friendliesBetween = ray & friendlies;
            // If there is exactly one friendly piece between the king and the pinner, it's pinned
            if (Bits.count(friendliesBetween) == 1) {
                int friendlySquare = Bits.next(friendliesBetween);
                this.pinMask |= friendliesBetween;
                this.pinRayMasks[friendlySquare] = ray | (Bits.of(possiblePinner));
            }

            possiblePinners = Bits.pop(possiblePinners);
        }

    }

    public long calculateThreats(Board board, boolean white) {

        long threats = 0L;
        long occ = board.getOccupied();

        long knights = board.getKnights(white);
        while (knights != 0) {
            final int square = Bits.next(knights);
            threats |= Attacks.knightAttacks(square);
            knights = Bits.pop(knights);
        }

        long bishops = board.getBishops(white) | board.getQueens(white);
        while (bishops != 0) {
            final int square = Bits.next(bishops);
            threats |= Attacks.bishopAttacks(square, occ);
            bishops = Bits.pop(bishops);
        }

        long rooks = board.getRooks(white) | board.getQueens(white);
        while (rooks != 0) {
            final int square = Bits.next(rooks);
            threats |= Attacks.rookAttacks(square, occ);
            rooks = Bits.pop(rooks);
        }

        long pawns = board.getPawns(white);
        threats |= Attacks.pawnAttacks(pawns, white);

        long king = board.getKing(white);
        final int square = Bits.next(king);
        threats |= Attacks.kingAttacks(square);

        return threats;

    }

    public boolean isPseudoLegal(Board board, Move move) {

        if (move == null)
            return false;

        final boolean white = board.isWhite();
        final int from = move.from();
        final int to = move.to();
        final Piece piece = board.pieceAt(from);
        final long occupied = board.getOccupied();

        // Can't move without a piece
        if (piece == null)
            return false;

        // Can't move from an empty square
        if (!Bits.contains(board.getPieces(white), from))
            return false;

        Piece captured = board.pieceAt(to);
        if (captured != null) {

            // Can't capture our own piece
            if (Bits.contains(board.getPieces(white), to))
                return false;

            // Can't capture a king
            if (Bits.contains(board.getKings(), to))
                return false;

        }

        if (move.isCastling()) {

            // Can only castle with a king
            if (piece != Piece.KING)
                return false;

            // Must be castling on the home rank
            long rank = white ? Rank.FIRST : Rank.EIGHTH;
            if (!Bits.contains(rank, from) || !Bits.contains(rank, to))
                return false;

            int kingsideCastleSquare = white ? 6 : 62;
            int queensideCastleSquare = white ? 2 : 58;

            // Must be valid castling squares
            if (to != kingsideCastleSquare && to != queensideCastleSquare)
                return false;

            // Must have kingside rights
            if (to == kingsideCastleSquare && !board.getState().isKingsideCastlingAllowed(white))
                return false;

            // Must have queenside rights
            if (to == queensideCastleSquare && !board.getState().isQueensideCastlingAllowed(white))
                return false;

            boolean kingside = to == kingsideCastleSquare;

            final long travelSquares = getCastleTravelSquares(white, kingside);
            final long blockedSquares = travelSquares & occupied;
            final long safeSquares = getCastleSafeSquares(white, kingside);

            // Can't castle through check
            return blockedSquares == 0 && !isAttacked(board, white, safeSquares);

        }

        if (piece == Piece.PAWN) {

            if (move.isEnPassant()) {

                // Can't en passant if there's no en passant square
                if (board.getState().getEnPassantFile() < 0)
                    return false;

                final int epSquare = white ? to - 8 : to + 8;

                // Can't en passant if there's no enemy pawn to capture
                if (!Bits.contains(board.getPawns(!white), epSquare))
                    return false;

            }

            int fromRank = Rank.of(from);
            int toRank = Rank.of(to);

            // Can't move backwards
            if ((white && fromRank >= toRank) || (!white && fromRank <= toRank))
                return false;

            // Must promote on the promo rank, and can't promote on any other rank
            long promoRank = white ? Rank.EIGHTH : Rank.FIRST;
            if (move.isPromotion() != Bits.contains(promoRank, to))
                return false;

            int fromFile = File.of(from);
            int toFile = File.of(to);

            // Pawn captures
            if (fromFile != toFile) {

                // Must capture on an adjacent file
                if (toFile != fromFile + 1 && toFile != fromFile - 1)
                    return false;

                // Must be capturing a piece
                return captured != null || move.isEnPassant();

            } else {
                // Can't capture a piece with a pawn push
                if (captured != null)
                    return false;

                if (move.isPawnDoubleMove()) {

                    // Can't double push from the wrong rank
                    long startRank = white ? Rank.SECOND : Rank.SEVENTH;
                    if (!Bits.contains(startRank, from))
                        return false;

                    // Can't double push if there's a piece in the way
                    int betweenSquare = white ? from + 8 : from - 8;
                    return !Bits.contains(occupied, betweenSquare);

                }
            }

        } else {

            // Can't make pawn-specific moves with a non-pawn
            if (move.isPawnDoubleMove() || move.isEnPassant() || move.isPromotion())
                return false;

            long attacks = switch (piece) {
                case KNIGHT ->  getKnightAttacks(board, from, white);
                case BISHOP ->  getBishopAttacks(board, from, white);
                case ROOK ->    getRookAttacks(board, from, white);
                case QUEEN ->   getQueenAttacks(board, from, white);
                case KING ->    getKingAttacks(board, from, white);
                default -> 0L;
            };

            // Must be a valid move for the piece
            return Bits.contains(attacks, to);

        }

        return true;

    }

    public boolean isLegal(Board board, Move move) {
        if (!isPseudoLegal(board, move))
            return false;

        board.makeMove(move);
        boolean legal = !isCheck(board, !board.isWhite());
        board.unmakeMove();

        return legal;
    }

    private List<Move> getPromotionMoves(int from, int to) {
        return List.of(new Move(from, to, Move.PROMOTE_TO_QUEEN_FLAG),
                        new Move(from, to, Move.PROMOTE_TO_ROOK_FLAG),
                        new Move(from, to, Move.PROMOTE_TO_BISHOP_FLAG),
                        new Move(from, to, Move.PROMOTE_TO_KNIGHT_FLAG));
    }


    private boolean leavesKingInCheck(Board board, Move move, boolean white) {
        board.makeMove(move);
        final int kingSquare = white ? Bits.next(board.getKing(true)) : Bits.next(board.getKing(false));
        final boolean isAttacked = isAttacked(board, white, Bits.of(kingSquare));
        board.unmakeMove();
        return isAttacked;
    }

    private boolean isPinned(int from) {
        return (Bits.of(from) & pinMask) != 0;
    }

    private boolean isMovingAlongPinRay(int from, int to) {
        final long pinRay = pinRayMasks[from];
        return (Bits.of(to) & pinRay) != 0;
    }

    private long getCastleTravelSquares(boolean white, boolean isKingside) {
        if (isKingside) return white ? Castling.WHITE_KINGSIDE_TRAVEL_MASK : Castling.BLACK_KINGSIDE_TRAVEL_MASK;
        else return white ? Castling.WHITE_QUEENSIDE_TRAVEL_MASK : Castling.BLACK_QUEENSIDE_TRAVEL_MASK;
    }

    private long getCastleSafeSquares(boolean white, boolean isKingside) {
        if (isKingside) return white ? Castling.WHITE_KINGSIDE_SAFE_MASK : Castling.BLACK_KINGSIDE_SAFE_MASK;
        else return white ? Castling.WHITE_QUEENSIDE_SAFE_MASK : Castling.BLACK_QUEENSIDE_SAFE_MASK;
    }

    private int getCastleEndSquare(boolean white, boolean isKingside) {
        if (isKingside) return white ? 6 : 62;
        else return white ? 2 : 58;
    }

    public long getPinMask() {
        return pinMask;
    }

    /**
     * Estimate the number of legal moves in the current position, based on the piece count and
     * the average number of legal moves per piece. Used to initialise the legal moves ArrayList
     * with a 'best guess', to reduce the number of times the ArrayList has to grow during move
     * generation, yielding a small increase in performance.
     */
    private int estimateLegalMoves() {
        return (Bits.count(pawns) * 2) +
                (Bits.count(knights) * 3) +
                (Bits.count(bishops) * 3) +
                (Bits.count(rooks) * 6) +
                (Bits.count(queens) * 9) +
                (Bits.count(king) * 3);
    }

    private void initPieces(Board board, boolean white) {
        this.pawns = board.getPawns(white);
        this.knights = board.getKnights(white);
        this.bishops = board.getBishops(white);
        this.rooks = board.getRooks(white);
        this.queens = board.getQueens(white);
        this.king = board.getKing(white);
    }

    public enum MoveFilter {
        ALL,
        NOISY,
        QUIET,
        CAPTURES_ONLY,
    }
}
