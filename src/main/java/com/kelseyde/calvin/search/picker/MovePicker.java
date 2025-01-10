package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;

import java.util.List;

/**
 * Selects the next move to try in a given position. Moves are selected in stages. First, the 'best' move from the
 * transposition table is tried before any moves are generated. Then, the noisy moves are generated and separated into
 * 'good' and 'bad' noisies. Finally, the quiet moves are generated.
 * </p>
 * Within each stage, the order in which moves are tried is determined by several heuristics in the {@link MoveScorer}.
 * </p>
 * The idea behind generating and selecting moves in stages is to save time. Since in most positions only a few moves -
 * or even only a single move - will be tried, the time spent generating all the other moves is essentially wasted.
 */
public class MovePicker {

    public enum Stage {
        TT_MOVE,
        GEN_NOISY,
        GOOD_NOISY,
        KILLER,
        GEN_QUIET,
        QUIET,
        BAD_NOISY,

        QSEARCH_GEN_NOISY,
        QSEARCH_NOISY,

        END
    }

    final MoveGenerator movegen;
    final MoveScorer scorer;
    final SearchHistory history;

    final Move ttMove;
    final Board board;
    final int ply;

    Stage stage;
    boolean skipQuiets;
    final boolean inCheck;

    int moveIndex;
    int killerIndex;

    ScoredMove[] goodNoisies;
    ScoredMove[] badNoisies;
    ScoredMove[] quiets;

    public MovePicker(EngineConfig config, MoveGenerator movegen, SearchStack ss, SearchHistory history,
                      Board board, int ply, Move ttMove, boolean inCheck) {
        this.movegen = movegen;
        this.scorer = new MoveScorer(config, history, ss);
        this.history = history;
        this.board = board;
        this.ply = ply;
        this.ttMove = ttMove;
        this.inCheck = inCheck;
        this.stage = ttMove != null ? Stage.TT_MOVE : Stage.GEN_NOISY;
    }

    public ScoredMove next() {

        ScoredMove nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE ->     pickTTMove(Stage.GEN_NOISY);
                case GEN_NOISY ->   generate(MoveFilter.NOISY, Stage.GOOD_NOISY);
                case GOOD_NOISY ->  pickMove(Stage.KILLER);
                case KILLER ->      pickKiller(Stage.GEN_QUIET);
                case GEN_QUIET ->   generate(MoveFilter.QUIET, Stage.QUIET);
                case QUIET ->       pickMove(Stage.BAD_NOISY);
                case BAD_NOISY ->   pickMove(Stage.END);
                case END, QSEARCH_GEN_NOISY, QSEARCH_NOISY -> null;
            };
            if (stage == Stage.END) break;
        }
        return nextMove;

    }

    /**
     * Select the next move from the move list.
     * @param nextStage the next stage to move on to, if we have tried all moves in the current stage.
     */
    protected ScoredMove pickMove(Stage nextStage) {

        final ScoredMove[] moves = switch (stage) {
            case GOOD_NOISY, QSEARCH_NOISY -> goodNoisies;
            case BAD_NOISY -> badNoisies;
            case QUIET -> quiets;
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        };

        // If we're in check then all evasions have been tried in the noisy stage
        if (stage == Stage.QUIET && (skipQuiets || inCheck)) {
            return nextStage(nextStage);
        }
        if (moveIndex >= moves.length) {
            return nextStage(nextStage);
        }

        ScoredMove move = pick(moves);

        if (move == null) {
            return nextStage(nextStage);
        }

        moveIndex++;
        return move;

    }

    protected ScoredMove pickKiller(Stage nextStage) {

        Move[] killers = history.getKillerTable().getKillers(ply);
        if (killerIndex >= killers.length) {
            return nextStage(nextStage);
        }

        Move killer = killers[killerIndex++];

        // Skip the killer if it's also the TT move - it will be tried first
        if (killer == null || killer.equals(ttMove)) {
            return pickKiller(nextStage);
        }

        // Skip illegal killers
        if (!movegen.isLegal(board, killer)) {
            return pickKiller(nextStage);
        }

        return scorer.score(board, killer, ply, stage);
    }

    protected ScoredMove pickTTMove(Stage nextStage) {
        stage = nextStage;
        final Piece piece = board.pieceAt(ttMove.from());
        final Piece captured = ttMove.isEnPassant() ? Piece.PAWN : board.pieceAt(ttMove.to());
        return new ScoredMove(ttMove, piece, captured, 0, 0, MoveType.TT_MOVE);
    }

    protected ScoredMove generate(MoveFilter filter, Stage nextStage) {
        if (inCheck) {
            filter = MoveFilter.ALL;
            if (moves != null) {
                // Only one call to movegen is necessary in check
                stage = nextStage;
                return null;
            }
        }
        List<Move> stagedMoves = movegen.generateMoves(board, filter);

        if (stage == Stage.GEN_NOISY) {
            // In noisy movegen we separate the moves into 'good' and 'bad' noisies
            int goodIndex = 0;
            int badIndex = 0;
            goodNoisies = new ScoredMove[stagedMoves.size()];
            badNoisies = new ScoredMove[stagedMoves.size()];
            for (Move move : stagedMoves) {
                ScoredMove scoredMove = scorer.score(board, move, ply, stage);
                if (scoredMove.moveType() == MoveType.GOOD_NOISY) {
                    goodNoisies[goodIndex++] = scoredMove;
                } else {
                    badNoisies[badIndex++] = scoredMove;
                }
            }
        }
        else if (stage == Stage.GEN_QUIET) {
            // In quiet movegen everything is treated as a 'quiet' move
            int quietIndex = 0;
            quiets = new ScoredMove[stagedMoves.size()];
            for (Move move : stagedMoves) {
                ScoredMove scoredMove = scorer.score(board, move, ply, stage);
                quiets[quietIndex++] = scoredMove;
            }
        }
        else if (stage == Stage.QSEARCH_GEN_NOISY) {
            // In quiescent movegen all moves are treated as 'good noisies'
            goodNoisies = new ScoredMove[stagedMoves.size()];
            int goodIndex = 0;
            for (Move move : stagedMoves) {
                ScoredMove scoredMove = scorer.score(board, move, ply, stage);
                // In q-search, only consider good noisies
                // unless we are in check, in which case consider all moves.
                if (scoredMove.isGoodNoisy() || inCheck) {
                    goodNoisies[goodIndex++] = scoredMove;
                }
            }
        }

        moveIndex = 0;
        stage = nextStage;
        return null;
    }

    /**
     * Select the move with the highest score and move it to the head of the move list.
     */
    protected ScoredMove pick(ScoredMove[] moves) {
        if (moveIndex >= moves.length) {
            return null;
        }
        ScoredMove best = moves[moveIndex];
        if (best == null) {
            return null;
        }
        int bestScore = best.score();
        int bestIndex = moveIndex;
        for (int j = moveIndex + 1; j < moves.length; j++) {
            ScoredMove current = moves[j];
            if (current == null) {
                break;
            }
            if (current.score() > bestScore) {
                bestScore = current.score();
                bestIndex = j;
            }
        }
        if (bestIndex != moveIndex) {
            swap(moves, moveIndex, bestIndex);
        }
        ScoredMove scoredMove = moves[moveIndex];
        if (scoredMove == null || isSpecial(scoredMove.move())) {
            moveIndex++;
            return pick(moves);
        }
        return scoredMove;
    }

    protected void swap(ScoredMove[] moves, int i, int j) {
        ScoredMove temp = moves[i];
        moves[i] = moves[j];
        moves[j] = temp;
    }

    public void setSkipQuiets(boolean skipQuiets) {
        this.skipQuiets = skipQuiets;
    }

    private boolean isSpecial(Move move) {
        if (move.equals(ttMove)) {
            return true;
        }
        for (Move killer : history.getKillerTable().getKillers(ply)) {
            if (move.equals(killer)) {
                return true;
            }
        }
        return false;
    }

    private ScoredMove nextStage(Stage nextStage) {
        moveIndex = 0;
        stage = nextStage;
        return null;
    }

}
