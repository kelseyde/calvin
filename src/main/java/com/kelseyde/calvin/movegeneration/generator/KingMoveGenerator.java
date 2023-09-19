package com.kelseyde.calvin.movegeneration.generator;


import com.kelseyde.calvin.board.BitBoard;
import com.kelseyde.calvin.board.BitBoards;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.piece.PieceType;
import com.kelseyde.calvin.board.move.Move;
import com.kelseyde.calvin.board.move.MoveType;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KingMoveGenerator implements PseudoLegalMoveGenerator {

    @Getter
    private final PieceType pieceType = PieceType.KING;

    public static final long[] KING_ATTACKS = new long[] {
            0x0000000000000302L, 0x0000000000000705L, 0x0000000000000e0aL, 0x0000000000001c14L,
            0x0000000000003828L, 0x0000000000007050L, 0x000000000000e0a0L, 0x000000000000c040L,
            0x0000000000030203L, 0x0000000000070507L, 0x00000000000e0a0eL, 0x00000000001c141cL,
            0x0000000000382838L, 0x0000000000705070L, 0x0000000000e0a0e0L, 0x0000000000c040c0L,
            0x0000000003020300L, 0x0000000007050700L, 0x000000000e0a0e00L, 0x000000001c141c00L,
            0x0000000038283800L, 0x0000000070507000L, 0x00000000e0a0e000L, 0x00000000c040c000L,
            0x0000000302030000L, 0x0000000705070000L, 0x0000000e0a0e0000L, 0x0000001c141c0000L,
            0x0000003828380000L, 0x0000007050700000L, 0x000000e0a0e00000L, 0x000000c040c00000L,
            0x0000030203000000L, 0x0000070507000000L, 0x00000e0a0e000000L, 0x00001c141c000000L,
            0x0000382838000000L, 0x0000705070000000L, 0x0000e0a0e0000000L, 0x0000c040c0000000L,
            0x0003020300000000L, 0x0007050700000000L, 0x000e0a0e00000000L, 0x001c141c00000000L,
            0x0038283800000000L, 0x0070507000000000L, 0x00e0a0e000000000L, 0x00c040c000000000L,
            0x0302030000000000L, 0x0705070000000000L, 0x0e0a0e0000000000L, 0x1c141c0000000000L,
            0x3828380000000000L, 0x7050700000000000L, 0xe0a0e00000000000L, 0xc040c00000000000L,
            0x0203000000000000L, 0x0507000000000000L, 0x0a0e000000000000L, 0x141c000000000000L,
            0x2838000000000000L, 0x5070000000000000L, 0xa0e0000000000000L, 0x40c0000000000000L
    };

    public Set<Move> generatePseudoLegalMoves(Board board) {

        Set<Move> moves = new HashSet<>();

        long king = board.isWhiteToMove() ? board.getWhiteKing() : board.getBlackKing();
        if (king == 0L) {
            return Collections.emptySet();
        }
        long friendlyPieces = board.isWhiteToMove() ? board.getWhitePieces() : board.getBlackPieces();
        long occupied = board.getOccupied();

        int startSquare = BitBoard.scanForward(king);

        long kingMoves = KING_ATTACKS[startSquare] &~ friendlyPieces;
        while (kingMoves != 0) {
            int endSquare = BitBoard.scanForward(kingMoves);
            moves.add(move(startSquare, endSquare).build());
            kingMoves = BitBoard.popLSB(kingMoves);
        }
        boolean isKingsideAllowed = board.isWhiteToMove() ? board.isWhiteKingsideCastlingAllowed() : board.isBlackKingsideCastlingAllowed();
        if (isKingsideAllowed) {
            long travelSquares = board.isWhiteToMove() ? BitBoards.WHITE_KINGSIDE_CASTLE_TRAVEL_MASK : BitBoards.BLACK_KINGSIDE_CASTLE_TRAVEL_MASK;
            long blockedSquares = travelSquares & occupied;
            if (blockedSquares == 0) {
                int endSquare = board.isWhiteToMove() ? 6 : 62;
                moves.add(move(startSquare, endSquare).moveType(MoveType.KINGSIDE_CASTLE).build());
            }
        }
        boolean isQueensideAllowed = board.isWhiteToMove() ? board.isWhiteQueensideCastlingAllowed() : board.isBlackQueensideCastlingAllowed();
        if (isQueensideAllowed) {
            long travelSquares = board.isWhiteToMove() ? BitBoards.WHITE_QUEENSIDE_CASTLE_TRAVEL_MASK : BitBoards.BLACK_QUEENSIDE_CASTLE_TRAVEL_MASK;
            long blockedSquares = travelSquares & occupied;
            if (blockedSquares == 0) {
                int endSquare = board.isWhiteToMove() ? 2 : 58;
                moves.add(move(startSquare, endSquare).moveType(MoveType.QUEENSIDE_CASTLE).build());
            }
        }

        return moves;

    }

    private Move.MoveBuilder move(int startSquare, int endSquare) {
        return Move.builder()
                .pieceType(PieceType.KING)
                .startSquare(startSquare)
                .endSquare(endSquare);
    }

}
