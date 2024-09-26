package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;

public class QuiescentMovePicker extends MovePicker {

    private MoveFilter filter;

    public QuiescentMovePicker(
            MoveGenerator movegen, SearchStack ss, SearchHistory history, Board board, int ply, Move ttMove, boolean inCheck, long threats) {
        super(movegen, ss, history, board, ply, ttMove, inCheck, threats);
        this.skipQuiets = true;
    }

    @Override
    public Move pickNextMove() {

        Move nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE -> pickTTMove();
                case NOISY -> pickMove(filter, Stage.END);
                case QUIET, END -> null;
            };
            if (stage == Stage.END) break;
        }
        return nextMove;

    }

    public void setFilter(MoveFilter filter) {
        this.filter = filter;
    }

}
