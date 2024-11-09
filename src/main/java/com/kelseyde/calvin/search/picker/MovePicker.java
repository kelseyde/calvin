package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.search.PlayedMove;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.search.SearchStack.SearchStackEntry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects the next move to try in a given position. Moves are selected in stages. First, the 'best' move from the
 * transposition table is tried before any moves are generated. Then, all the 'noisy' moves are tried (captures,
 * checks and promotions). Finally, we generate the remaining quiet moves.
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
    final SearchHistory history;
    final SearchStack ss;

    final Move ttMove;
    final Board board;
    final int ply;

    Stage stage;
    boolean skipQuiets;
    boolean inCheck;

    int moveIndex;
    int killerIndex;

    ScoredMove[] goodNoisies;
    ScoredMove[] badNoisies;
    ScoredMove[] quiets;

    public MovePicker(
            MoveGenerator movegen, SearchStack ss, SearchHistory history, Board board, int ply, Move ttMove, boolean inCheck) {
        this.movegen = movegen;
        this.history = history;
        this.board = board;
        this.ss = ss;
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
                case KILLER ->      pickKiller(Stage.BAD_NOISY);
                case BAD_NOISY ->   pickMove(Stage.GEN_QUIET);
                case GEN_QUIET ->   generate(MoveFilter.QUIET, Stage.QUIET);
                case QUIET ->       pickMove(Stage.END);
                case END,
                     QSEARCH_GEN_NOISY,
                     QSEARCH_NOISY -> null;
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

        return scoreMove(board, killer, ttMove, ply);
    }

    protected ScoredMove pickTTMove(Stage nextStage) {
        stage = nextStage;
        final Piece piece = board.pieceAt(ttMove.from());
        final Piece captured = ttMove.isEnPassant() ? Piece.PAWN : board.pieceAt(ttMove.to());
        return new ScoredMove(ttMove, piece, captured, MoveType.TT_MOVE.bonus, 0, MoveType.TT_MOVE);
    }

    protected ScoredMove generate(MoveFilter filter, Stage nextStage) {
        List<Move> stagedMoves = movegen.generateMoves(board, filter);

        if (stage == Stage.GEN_NOISY) {
            // In noisy movegen we separate the moves into 'good' and 'bad' noisies
            int goodIndex = 0;
            int badIndex = 0;
            goodNoisies = new ScoredMove[stagedMoves.size()];
            badNoisies = new ScoredMove[stagedMoves.size()];
            for (Move move : stagedMoves) {
                ScoredMove scoredMove = scoreMove(board, move, ttMove, ply);
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
                ScoredMove scoredMove = scoreMove(board, move, ttMove, ply);
                quiets[quietIndex++] = scoredMove;
            }
        }
        else if (stage == Stage.QSEARCH_GEN_NOISY) {
            // In quiescent movegen all moves are treated as 'good noisies'
            goodNoisies = new ScoredMove[stagedMoves.size()];
            int goodIndex = 0;
            for (Move move : stagedMoves) {
                ScoredMove scoredMove = scoreMove(board, move, ttMove, ply);
                goodNoisies[goodIndex++] = scoredMove;
            }
        }

        moveIndex = 0;
        stage = nextStage;
        return null;
    }

    protected ScoredMove scoreMove(Board board, Move move, Move ttMove, int ply) {

        final int from = move.from();
        final int to = move.to();

        final Piece piece = board.pieceAt(from);
        final Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(to);

        final boolean capture = captured != null;
        final boolean promotion = move.isPromotion();
        final boolean quietCheck = stage == Stage.GEN_NOISY && !promotion && !capture;
        final boolean noisy = quietCheck || capture || promotion;

        if (noisy) {
            return scoreNoisy(board, move, piece, captured, quietCheck);
        } else {
            return scoreQuiet(board, move, piece, captured, ply);
        }

    }

    protected ScoredMove scoreNoisy(Board board, Move move, Piece piece, Piece captured, boolean quietCheck) {

        final boolean white = board.isWhite();

        int noisyScore = 0;

        if (move.isPromotion()) {
            // Queen promos are treated as 'good noisies', under promotions as 'bad noisies'
            final MoveType type = move.promoPiece() == Piece.QUEEN ? MoveType.GOOD_NOISY : MoveType.BAD_NOISY;
            noisyScore += type.bonus;
            return new ScoredMove(move, piece, captured, noisyScore, 0, type);
        }

        if (quietCheck) {
            // Quiet checks are treated as 'bad noisies' and scored using quiet history heuristics
            final MoveType type = MoveType.BAD_NOISY;
            final int historyScore = history.getQuietHistoryTable().get(move, piece, white);
            final int contHistScore = continuationHistoryScore(move, piece, white);
            noisyScore = type.bonus + historyScore + contHistScore;
            return new ScoredMove(move, piece, captured, noisyScore, historyScore, type);
        }

        // Separate good and bad noisies based on the MVV-LVA ('most valuable victim, least valuable attacker') heuristic
        final int materialDelta = captured.value() - piece.value();
        final MoveType type = materialDelta >= 0 ? MoveType.GOOD_NOISY : MoveType.BAD_NOISY;

        noisyScore += type.bonus;

        // Add MVV score to the noisy score
        noisyScore += MoveType.MVV_OFFSET * captured.index();

        // Tie-break with capture history
        final int historyScore = history.getCaptureHistoryTable().get(piece, move.to(), captured, board.isWhite());
        noisyScore += historyScore;

        return new ScoredMove(move, piece, captured, noisyScore, historyScore, type);
    }

    protected ScoredMove scoreQuiet(Board board, Move move, Piece piece, Piece captured, int ply) {
        boolean white = board.isWhite();
        int historyScore = history.getQuietHistoryTable().get(move, piece, white);
        int contHistScore = continuationHistoryScore(move, piece, white);
        int score = MoveType.QUIET.bonus + historyScore + contHistScore;
        return new ScoredMove(move, piece, captured, score, historyScore, MoveType.QUIET);
    }

    int continuationHistoryScore(Move move, Piece piece, boolean white) {
        int contHistScore = 0;
        // Get the continuation history score for the move
        SearchStackEntry prevEntry = ss.get(ply - 1);
        if (prevEntry != null && prevEntry.currentMove != null) {
            PlayedMove prevMove = prevEntry.currentMove;
            contHistScore = history.getContHistTable().get(prevMove.move, prevMove.piece, move, piece, white);
        }

        SearchStackEntry prevEntry2 = ss.get(ply - 2);
        if (prevEntry2 != null && prevEntry2.currentMove != null) {
            PlayedMove prevMove2 = prevEntry2.currentMove;
            contHistScore += history.getContHistTable().get(prevMove2.move, prevMove2.piece, move, piece, white);
        }
        return contHistScore;
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
