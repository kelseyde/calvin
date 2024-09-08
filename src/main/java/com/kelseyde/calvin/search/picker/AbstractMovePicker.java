package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import lombok.Setter;

import java.util.List;

public abstract class AbstractMovePicker {

    public enum Stage {
        TT_MOVE,
        NOISY,
        QUIET,
        END
    }

    MoveGeneration moveGenerator;
    MoveOrdering moveOrderer;
    Board board;

    @Setter Move ttMove;
    @Setter MoveGeneration.MoveFilter filter;

    int moveIndex;
    ScoredMove[] moves;

    abstract int scoreMove(Move move);

    public void scoreMoves(List<Move> stagedMoves) {
        moves = new ScoredMove[stagedMoves.size()];
        for (int i = 0; i < stagedMoves.size(); i++) {
            Move move = stagedMoves.get(i);
            int score = scoreMove(move);
            moves[i] = new ScoredMove(move, score);
        }
    }

    public Move pick() {
        for (int j = moveIndex + 1; j < moves.length; j++) {
            if (moves[j].score() > moves[moveIndex].score()) {
                swap(moveIndex, j);
            }
        }
        return moves[moveIndex].move();
    }

    private void swap(int i, int j) {
        ScoredMove temp = moves[i];
        moves[i] = moves[j];
        moves[j] = temp;
    }

}
