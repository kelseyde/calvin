package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;

import java.util.List;

public class QuiescentMovePicker extends MovePicker {

    private MoveFilter filter;

    public QuiescentMovePicker(
            MoveGenerator movegen, SearchStack ss, SearchHistory history, Board board, int ply, Move ttMove, boolean inCheck) {
        super(movegen, ss, history, board, ply, ttMove, inCheck);
        this.skipQuiets = true;
    }

    @Override
    public ScoredMove pickNextMove() {

        ScoredMove nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE -> pickTTMove();
                case GEN_NOISY -> generate(filter, Stage.GOOD_NOISY);
                case GOOD_NOISY -> pickMove(Stage.END);
                case GEN_QUIET, KILLER, BAD_NOISY, QUIET, START_GOOD_NOISY, START_BAD_NOISY, START_KILLER, START_QUIET, END -> null;
            };
            if (stage == Stage.END) break;
        }
        return nextMove;

    }

    @Override
    protected ScoredMove generate(MoveFilter filter, Stage nextStage) {
        List<Move> stagedMoves = movegen.generateMoves(board, filter);
        this.moves = new ScoredMove[stagedMoves.size()];
        for (int i = 0; i < stagedMoves.size(); i++) {
            Move move = stagedMoves.get(i);
            ScoredMove scoredMove = scoreMove(board, move, ttMove, ply);
            moves[i] = scoredMove;
        }
        moveIndex = 0;
        stage = nextStage;
        return null;
    }

    public void setFilter(MoveFilter filter) {
        this.filter = filter;
    }

}
