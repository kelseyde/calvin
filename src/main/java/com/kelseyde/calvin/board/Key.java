package com.kelseyde.calvin.board;

import com.kelseyde.calvin.board.Bits.Square;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating Zobrist keys, which are 64-bit values that (almost uniquely) represent a chess position.
 * It is used to quickly identify positions that have already been examined by the move generator/evaluator, cutting
 * down on a lot of double-work.
 *
 * @see <a href="https://www.chessprogramming.org/Zobrist_Hashing">Chess Programming Wiki</a>
 */
// TODO: try only generating key once for each update
public class Key {

    private static final int CASTLING_RIGHTS_COUNT = 16;
    private static final int EN_PASSANT_FILES_COUNT = 9;

    private static final long[][][] PIECE_SQUARE_HASH = new long[Square.COUNT][2][Piece.COUNT];
    private static final long[] CASTLING_RIGHTS = new long[CASTLING_RIGHTS_COUNT];
    private static final long[] EN_PASSANT_FILE = new long[EN_PASSANT_FILES_COUNT];
    private static final long SIDE_TO_MOVE;
    private static final int WHITE = 0;
    private static final int BLACK = 1;

    static {

        Random random = new Random(18061995);

        // Generate random Zobrist keys for each piece on each square
        for (int square = 0; square < Square.COUNT; square++) {
            for (int pieceIndex : Arrays.stream(Piece.values()).map(Piece::index).toList()) {
                PIECE_SQUARE_HASH[square][WHITE][pieceIndex] = random.nextLong();
                PIECE_SQUARE_HASH[square][BLACK][pieceIndex] = random.nextLong();
            }
        }

        // Generate random Zobrist keys for castling rights and en passant files
        for (int i = 0; i < CASTLING_RIGHTS.length; i++) {
            CASTLING_RIGHTS[i] = random.nextLong();
        }
        for (int i = 0; i < EN_PASSANT_FILE.length; i++) {
            EN_PASSANT_FILE[i] = random.nextLong();
        }

        // Generate random key for side to move
        SIDE_TO_MOVE = random.nextLong();
    }

    public static long generateKey(Board board) {
        long key = 0L;

        // Define arrays for each piece type and color
        long[][] pieces = {
                { board.getPawns(true), board.getPawns(false) },
                { board.getKnights(true), board.getKnights(false) },
                { board.getBishops(true), board.getBishops(false) },
                { board.getRooks(true), board.getRooks(false) },
                { board.getQueens(true), board.getQueens(false) },
                { board.getKing(true), board.getKing(false) }
        };

        // Loop through each square and piece type
        for (int square = 0; square < Square.COUNT; square++) {
            for (int pieceType = 0; pieceType < Piece.COUNT; pieceType++) {
                key = updateKeyForPiece(key, pieces[pieceType][WHITE], pieces[pieceType][BLACK], square, pieceType);
            }
        }

        // Update key with en passant, castling rights, and side to move
        key ^= EN_PASSANT_FILE[board.getState().getEnPassantFile() + 1];
        key ^= castling(board.getState().getRights());
        if (board.isWhite()) {
            key ^= SIDE_TO_MOVE;
        }

        return key;
    }

    public static long generatePawnKey(Board board) {
        long key = 0L;

        // Get the bitboards for white and black pawns
        long whitePawns = board.getPawns(true);
        long blackPawns = board.getPawns(false);

        // Loop through each square
        if (whitePawns != 0L || blackPawns != 0L) {  // Early exit optimization if no pawns
            for (int square = 0; square < Square.COUNT; square++) {
                key = updateKeyForPiece(key, whitePawns, blackPawns, square, Piece.PAWN.index());
            }
        }

        return key;
    }

    public static long[] generateNonPawnKeys(Board board) {
        long[] keys = new long[2];

        // Array of piece types and their corresponding bitboards for both sides
        long[][] nonPawnPieces = {
                { board.getKnights(true), board.getKnights(false) },
                { board.getBishops(true), board.getBishops(false) },
                { board.getRooks(true), board.getRooks(false) },
                { board.getQueens(true), board.getQueens(false) },
                { board.getKing(true), board.getKing(false) }
        };

        // Array of corresponding piece indices
        int[] pieceIndices = {
                Piece.KNIGHT.index(), Piece.BISHOP.index(),
                Piece.ROOK.index(), Piece.QUEEN.index(), Piece.KING.index()
        };

        // Loop through each square and update the keys based on the pieces on the board
        for (int square = 0; square < 64; square++) {
            for (int i = 0; i < nonPawnPieces.length; i++) {
                if (((nonPawnPieces[i][WHITE] >>> square) & 1) == 1) {
                    keys[WHITE] ^= PIECE_SQUARE_HASH[square][WHITE][pieceIndices[i]];
                } else if (((nonPawnPieces[i][BLACK] >>> square) & 1) == 1) {
                    keys[BLACK] ^= PIECE_SQUARE_HASH[square][BLACK][pieceIndices[i]];
                }
            }
        }

        return keys;
    }

    private static long updateKeyForPiece(long key, long whiteBitboard, long blackBitboard, int square, int pieceIndex) {
        if (((whiteBitboard >>> square) & 1) == 1) {
            key ^= PIECE_SQUARE_HASH[square][WHITE][pieceIndex];
        } else if (((blackBitboard >>> square) & 1) == 1) {
            key ^= PIECE_SQUARE_HASH[square][BLACK][pieceIndex];
        }
        return key;
    }

    public static long piece(int from, int to, Piece pieceType, boolean white) {
        return PIECE_SQUARE_HASH[from][Colour.index(white)][pieceType.index()]
                ^ PIECE_SQUARE_HASH[to][Colour.index(white)][pieceType.index()];
    }

    public static long piece(int square, Piece pieceType, boolean white) {
        return PIECE_SQUARE_HASH[square][Colour.index(white)][pieceType.index()];
    }

    public static long rights(int oldCastlingRights, int newCastlingRights) {
        return castling(oldCastlingRights) ^ castling(newCastlingRights);
    }

    public static long enPassant(int oldEnPassantFile, int newEnPassantFile) {
        return EN_PASSANT_FILE[oldEnPassantFile + 1] ^ EN_PASSANT_FILE[newEnPassantFile + 1];
    }

    public static long sideToMove() {
        return SIDE_TO_MOVE;
    }

    public static long nullMove(int oldEnPassantFile) {
        return EN_PASSANT_FILE[oldEnPassantFile + 1] ^ EN_PASSANT_FILE[0] ^ SIDE_TO_MOVE;
    }

    private static long castling(int rights) {
        final int whiteShort = 0x04;
        final int whiteLong = 0x08;
        final int blackShort = 0x01;
        final int blackLong = 0x02;

        int flags = 0;
        if (Castling.kingsideAllowed(rights, true)) {
            flags |= whiteShort;
        }
        if (Castling.queensideAllowed(rights, true)) {
            flags |= whiteLong;
        }
        if (Castling.kingsideAllowed(rights, false)) {
            flags |= blackShort;
        }
        if (Castling.queensideAllowed(rights, false)) {
            flags |= blackLong;
        }
        return CASTLING_RIGHTS[flags];

    }

}