package com.kelseyde.calvin.model;

public class Piece {

    public static String getPieceCode(boolean isWhiteToMove, PieceType type) {
        String colourCode = isWhiteToMove ? "w" : "b";
        String pieceCode = type.pieceCode;
        return colourCode + pieceCode;
    }

}
