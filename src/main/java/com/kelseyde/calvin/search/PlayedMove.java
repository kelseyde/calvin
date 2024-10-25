package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;

public class PlayedMove {

    public final Move move;
    public final Piece piece;
    public final Piece captured;
    public int score;
    public boolean quiet;
    public boolean capture;

    public PlayedMove(Move move, Piece piece, Piece captured) {
        this.move = move;
        this.piece = piece;
        this.captured = captured;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isCapture() {
        return capture;
    }

}
