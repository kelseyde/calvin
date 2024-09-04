package com.kelseyde.calvin.board;

import java.util.Optional;

/**
 * Represents a single chess move.
 * All the information required to represent a move is encoded in a 16-bit integer value field.
 * <p>
 * The encoding of the move information in the value field is as follows:
 * <ul>
 *     <li>Bits 0 - 5: Start square (0 - 63)</li>
 *     <li>Bits 6 - 11: End square (0 - 63)</li>
 *     <li>Bits 12 - 15: Special move flags (promotion, castling, pawn double moves, en passant)</li>
 * </ul>
 * For more information on move encoding, see <a href="https://www.chessprogramming.org/Encoding_Moves">Chess Programming Wiki</a>.
 * <p>
 * Largely inspired by Sebastian Lague's Chess Coding Adventure:
 * @see <a href="https://github.com/SebLague/Chess-Coding-Adventure">Chess Coding Adventure</a>
 */
public record Move(int value) {

    // Special move flags
    public static final short NO_FLAG = 0b0000;
    public static final short EN_PASSANT_FLAG = 0b0001;
    public static final short CASTLE_FLAG = 0b0010;
    public static final short PAWN_DOUBLE_MOVE_FLAG = 0b0011;
    public static final short PROMOTE_TO_QUEEN_FLAG = 0b0100;
    public static final short PROMOTE_TO_KNIGHT_FLAG = 0b0101;
    public static final short PROMOTE_TO_ROOK_FLAG = 0b0110;
    public static final short PROMOTE_TO_BISHOP_FLAG = 0b0111;

    // Masks for extracting start and end squares from the value field
    public static final int FROM_MASK = 0b0000000000111111;
    public static final int TO_MASK = 0b0000111111000000;

    /**
     * Constructs a Move instance from the given start square and end square.
     *
     * @param from The starting square of the move (0 - 63).
     * @param to   The ending square of the move (0 - 63).
     */
    public Move(int from, int to) {
        this(from | to << 6);
    }

    /**
     * Constructs a Move instance from the given start square, end square, and move flag.
     *
     * @param from The starting square of the move (0 - 63).
     * @param to   The ending square of the move (0 - 63).
     * @param flag        The special move flag representing additional move information.
     */
    public Move(int from, int to, int flag) {
        this(from | (to << 6) | (flag << 12));
    }

    /**
     * Retrieves the starting square of the move.
     *
     * @return The starting square of the move (0 - 63).
     */
    public int getFrom() {
        return value & FROM_MASK;
    }

    /**
     * Retrieves the ending square of the move.
     *
     * @return The ending square of the move (0 - 63).
     */
    public int getTo() {
        return (value & TO_MASK) >>> 6;
    }

    /**
     * Retrieves the piece type to which a pawn is promoted in this move, if applicable.
     *
     * @return The piece type to which a pawn is promoted, or null if no promotion occurs.
     */
    public Piece getPromotionPiece() {
        return switch (value >>> 12) {
            case PROMOTE_TO_QUEEN_FLAG -> Piece.QUEEN;
            case PROMOTE_TO_ROOK_FLAG -> Piece.ROOK;
            case PROMOTE_TO_BISHOP_FLAG -> Piece.BISHOP;
            case PROMOTE_TO_KNIGHT_FLAG -> Piece.KNIGHT;
            default -> null;
        };
    }

    /**
     * Retrieves the special move flag representing additional move information.
     *
     * @param pieceType The type of piece to which a pawn is promoted.
     * @return The special move flag indicating pawn promotion.
     */
    public static short getPromotionFlag(Piece pieceType) {
        if (pieceType == null) {
            return NO_FLAG;
        }
        return switch (pieceType) {
            case QUEEN -> PROMOTE_TO_QUEEN_FLAG;
            case ROOK -> PROMOTE_TO_ROOK_FLAG;
            case BISHOP -> PROMOTE_TO_BISHOP_FLAG;
            case KNIGHT -> PROMOTE_TO_KNIGHT_FLAG;
            default -> NO_FLAG;
        };
    }

    /**
     * Checks if this move represents a pawn promotion.
     *
     * @return True if the move is a pawn promotion, false otherwise.
     */
    public boolean isPromotion() {
        return (value >>> 12) >= PROMOTE_TO_QUEEN_FLAG;
    }

    /**
     * Checks if this move represents an en passant capture.
     *
     * @return True if the move is an en passant capture, false otherwise.
     */
    public boolean isEnPassant() {
        return (value >>> 12) == EN_PASSANT_FLAG;
    }

    /**
     * Checks if this move represents castling.
     *
     * @return True if the move is a castling move, false otherwise.
     */
    public boolean isCastling() {
        return (value >>> 12) == CASTLE_FLAG;
    }

    /**
     * Checks if this move represents a pawn double move.
     *
     * @return True if the move is a pawn double move, false otherwise.
     */
    public boolean isPawnDoubleMove() {
        return (value >>> 12) == PAWN_DOUBLE_MOVE_FLAG;
    }

    /**
     * Checks if this move matches another move.
     *
     * @param move The move to compare against.
     * @return True if the moves match, false otherwise.
     */
    // TODO Optional is slow, replace it
    public boolean matches(Move move) {
        if (move == null) return false;
        boolean squareMatch = getFrom() == move.getFrom() && getTo() == move.getTo();
        boolean promotionMatch = Optional.ofNullable(getPromotionPiece())
                .map(piece -> piece.equals(move.getPromotionPiece()))
                .orElse(true);
        return squareMatch && promotionMatch;
    }
}
