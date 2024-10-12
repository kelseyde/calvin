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
            MoveGenerator movegen, SearchStack ss, SearchHistory history, Board board, int ply, Move ttMove, boolean inCheck) {
        super(movegen, ss, history, board, ply, ttMove, inCheck);
        this.skipQuiets = true;
    }

    @Override
    public Move pickNextMove() {

        short nextMove = 0;
        while (nextMove == 0) {
            nextMove = switch (stage) {
                case TT_MOVE -> pickTTMove();
                case GEN_NOISY -> generate(filter, Stage.NOISY);
                case NOISY -> pickMove(Stage.END);
                case GEN_QUIET, QUIET, END -> 0;
            };
            if (stage == Stage.END) break;
        }
        return nextMove != 0 ? new Move(nextMove) : null;

    }

    public void setFilter(MoveFilter filter) {
        this.filter = filter;
    }

}
