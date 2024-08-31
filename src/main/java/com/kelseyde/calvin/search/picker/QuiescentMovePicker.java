package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGeneration.MoveFilter;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Selects the next move to try in a position during quiescence search. First the move from the transposition table is
 * tried before any moves are generated. Then, all the noisy moves are generated and tried in order of their MVV-LVA score.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuiescentMovePicker implements MovePicking {

    public enum Stage {
        TT_MOVE,
        NOISY,
        END
    }

    final MoveGeneration moveGenerator;
    final MoveOrdering moveOrderer;

    final Board board;
    @Setter
    MoveFilter filter;
    Stage stage;

    @Setter
    Move ttMove;
    ScoredMove[] moves;
    int moveIndex;

    /**
     * Constructs a MovePicker with the specified move generator, move orderer, board, and ply.
     *
     * @param moveGenerator the move generator to use for generating moves
     * @param moveOrderer   the move orderer to use for scoring and ordering moves
     * @param board         the current state of the board
     */
    public QuiescentMovePicker(MoveGeneration moveGenerator, MoveOrdering moveOrderer, Board board) {
        this.moveGenerator = moveGenerator;
        this.moveOrderer = moveOrderer;
        this.board = board;
        this.stage = Stage.TT_MOVE;
    }

    /**
     * Picks the next move to make, cycling through the different stages until a move is found.
     *
     * @return the next move, or null if no moves are available
     */
    @Override
    public Move pickNextMove() {

        Move nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE -> pickTTMove();
                case NOISY -> pickMove();
                case END -> null;
            };
            if (stage == Stage.END) break;
        }
        return nextMove;

    }

    /**
     * Select the best move from the transposition table and advance to the next stage.
     */
    private Move pickTTMove() {
        stage = Stage.NOISY;
        return ttMove;
    }

    /**
     * Select the next move from the move list.
     */
    private Move pickMove() {

        if (moves == null) {
            List<Move> stagedMoves = moveGenerator.generateMoves(board, filter);
            scoreMoves(stagedMoves);
            moveIndex = 0;
        }
        if (moveIndex >= moves.length) {
            moves = null;
            stage = Stage.END;
            return null;
        }
        Move move = pick();
        moveIndex++;
        if (move.equals(ttMove)) {
            // Skip to the next move
            return pickMove();
        }
        return move;

    }

    /**
     * Moves are scored using the {@link MoveOrderer} MVV-LVA routine.
     */
    public void scoreMoves(List<Move> stagedMoves) {
        moves = new ScoredMove[stagedMoves.size()];
        for (int i = 0; i < stagedMoves.size(); i++) {
            moves[i] = new ScoredMove(stagedMoves.get(i), moveOrderer.mvvLva(board, stagedMoves.get(i), ttMove));
        }
    }

    /**
     * Select the move with the highest score and move it to the head of the move list.
     */
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
