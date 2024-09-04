package com.kelseyde.calvin.utils;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;

import java.util.List;
import java.util.Map;

public class Notation {

    public static final Map<Piece, String> PIECE_CODE_INDEX = Map.of(
            Piece.PAWN, "p",
            Piece.KNIGHT, "n",
            Piece.BISHOP, "b",
            Piece.ROOK, "r",
            Piece.QUEEN, "q",
            Piece.KING, "k"
    );

    public static final Map<Integer, String> FILE_CHAR_MAP = Map.of(
            0, "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h"
    );

    public static final Map<Integer, String> RANK_CHAR_MAP = Map.of(
            0, "1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8"
    );

    /**
     * Generate a {@link Move} from algebraic notation of the start and end square (e.g. "e2", "e4" -> new Move(12, 28))
     */
    public static Move fromNotation(String startSquare, String endSquare) {
        return new Move(fromNotation(startSquare), fromNotation(endSquare));
    }

    public static Move fromNotation(String startSquare, String endSquare, int flag) {
        return new Move(fromNotation(startSquare), fromNotation(endSquare), flag);
    }

    /**
     * Generate a {@link Move} from combined algebraic notation (e.g. "e2e4"), as used in the UCI protocol.
     * Special case promotion: "a2a1q" - values 'q' | 'b' | 'r' | 'n'
     */
    public static Move fromUCI(String notation) {
        int startSquare = fromNotation(notation.substring(0, 2));
        int endSquare = fromNotation(notation.substring(2, 4));

        int flag = Move.NO_FLAG;
        if (notation.length() == 5) {
            String pieceCode = notation.substring(4, 5);
            Piece promotionPieceType = PIECE_CODE_INDEX.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(pieceCode))
                    .findAny().orElseThrow().getKey();
            flag = Move.getPromotionFlag(promotionPieceType);
        }
        return new Move(startSquare, endSquare, flag);
    }

    public static String toNotation(Move move) {
        if (move == null) {
            return "-";
        }
        String notation = toNotation(move.getFrom()) + toNotation(move.getTo());
        if (move.getPromotionPiece() != null) {
            notation += PIECE_CODE_INDEX.get(move.getPromotionPiece());
        }
        return notation;
    }

    /**
     * Generate a square co-ordinate from algebraic notation (e.g. "e4" -> 28)
     */
    public static int fromNotation(String algebraic) {
        int xOffset = List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').indexOf(algebraic.charAt(0));
        int yAxis = (Integer.parseInt(Character.valueOf(algebraic.charAt(1)).toString()) - 1) * 8;
        return yAxis + xOffset;
    }

    public static String toNotation(int sq) {
        return getFileChar(sq) + getRankChar(sq);
    }

    public static String getRankChar(int sq) {
        return RANK_CHAR_MAP.get(Board.rank(sq));
    }

    public static String getFileChar(int sq) {
        return FILE_CHAR_MAP.get(Board.file(sq));
    }

}
