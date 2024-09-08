package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGeneration.MoveFilter;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Selects the next move to try in a given position. Moves are selected in stages. First, the 'best' move from the
 * transposition table is tried before any moves are generated. Then, all the 'noisy' moves are tried (captures,
 * checks and promotions). Finally, the remaining quiet moves are generated.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovePicker extends AbstractMovePicker {

    final SearchStack ss;
    final int ply;

    Stage stage;
    @Setter boolean skipQuiets;
    @Setter boolean inCheck;

    /**
     * Constructs a MovePicker with the specified move generator, move orderer, board, and ply.
     *
     * @param moveGenerator the move generator to use for generating moves
     * @param moveOrderer   the move orderer to use for scoring and ordering moves
     * @param board         the current state of the board
     * @param ply           the number of ply from the root position
     */
    public MovePicker(MoveGeneration moveGenerator, MoveOrdering moveOrderer, Board board, SearchStack ss, int ply) {
        this.moveGenerator = moveGenerator;
        this.moveOrderer = moveOrderer;
        this.board = board;
        this.ss = ss;
        this.ply = ply;
        this.stage = Stage.TT_MOVE;
    }

    /**
     * Picks the next move to make, cycling through the different stages until a move is found.
     *
     * @return the next move, or null if no moves are available
     */
    public Move pickNextMove() {

        Move nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE -> pickTTMove();
                case NOISY -> pickMove(MoveFilter.NOISY, Stage.QUIET);
                case QUIET -> pickMove(MoveFilter.QUIET, Stage.END);
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
     * @param filter the move generation filter to use, if the moves are not yet generated
     * @param nextStage the next stage to move on to, if we have tried all moves in the current stage.
     */
    private Move pickMove(MoveFilter filter, Stage nextStage) {

        if (stage == Stage.QUIET && (skipQuiets || inCheck)) {
            stage = nextStage;
            moves = null;
            return null;
        }

        if (moves == null) {
            List<Move> stagedMoves = moveGenerator.generateMoves(board, filter);
            scoreMoves(stagedMoves);
            moveIndex = 0;
        }
        if (moveIndex >= moves.length) {
            moves = null;
            stage = nextStage;
            return null;
        }
        Move move = pick();
        moveIndex++;
        if (move.equals(ttMove)) {
            // Skip to the next move
            return pickMove(filter, nextStage);
        }
        return move;

    }

    @Override
    int scoreMove(Move move) {
        return move.equals(ttMove) ? Integer.MIN_VALUE : moveOrderer.scoreMove(board, ss, move, ttMove, ply);
    }
}
