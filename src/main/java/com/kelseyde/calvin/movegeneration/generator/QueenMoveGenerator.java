package com.kelseyde.calvin.movegeneration.generator;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.piece.PieceType;

public class QueenMoveGenerator extends SlidingMoveGenerator {

    @Override
    public PieceType getPieceType() {
        return PieceType.QUEEN;
    }

    @Override
    protected long getPieceBitboard(Board board) {
        return board.isWhiteToMove() ? board.getWhiteQueens() : board.getBlackQueens();
    }

    @Override
    protected boolean isOrthogonal() {
        return true;
    }

    @Override
    protected boolean isDiagonal() {
        return true;
    }

}
