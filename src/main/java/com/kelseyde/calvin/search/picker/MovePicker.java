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
import com.kelseyde.calvin.tables.history.KillerTable;

import java.util.List;

/**
 * Selects the next move to try in a given position. Moves are selected in stages. First, the 'best' move from the
 * transposition table is tried before any moves are generated. Then, all the 'noisy' moves are tried (captures,
 * checks and promotions). Finally, we generate the remaining quiet moves.
 */
public class MovePicker {

    public enum Stage {
        TT_MOVE,
        GEN_NOISY,
        NOISY,
        GEN_QUIET,
        QUIET,
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
    ScoredMove[] moves;

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

    public ScoredMove pickNextMove() {

        ScoredMove nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE ->     pickTTMove();
                case GEN_NOISY ->   generate(MoveFilter.NOISY, Stage.NOISY);
                case NOISY ->       pickMove(Stage.GEN_QUIET);
                case GEN_QUIET ->   generate(MoveFilter.QUIET, Stage.QUIET);
                case QUIET ->       pickMove(Stage.END);
                case END ->         null;
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

        if (stage == Stage.QUIET && (skipQuiets || inCheck)) {
            stage = nextStage;
            return null;
        }
        if (moveIndex >= moves.length) {
            stage = nextStage;
            return null;
        }
        ScoredMove move = pick();
        moveIndex++;
        return move;

    }

    protected ScoredMove pickTTMove() {
        stage = Stage.GEN_NOISY;
        final Piece piece = board.pieceAt(ttMove.from());
        final Piece captured = board.pieceAt(ttMove.to());
        return new ScoredMove(ttMove, piece, captured, MoveType.TT_MOVE.bonus, 0, MoveType.TT_MOVE);
    }

    protected ScoredMove generate(MoveFilter filter, Stage nextStage) {
        List<Move> stagedMoves = movegen.generateMoves(board, filter);
        scoreMoves(stagedMoves);
        moveIndex = 0;
        stage = nextStage;
        return null;
    }

    protected void scoreMoves(List<Move> stagedMoves) {
        moves = new ScoredMove[stagedMoves.size()];
        for (int i = 0; i < stagedMoves.size(); i++) {
            Move move = stagedMoves.get(i);
            ScoredMove scoredMove = scoreMove(board, move, ttMove, ply);
            moves[i] = scoredMove;
        }
    }

    protected ScoredMove scoreMove(Board board, Move move, Move ttMove, int ply) {

        final int from = move.from();
        final int to = move.to();

        final Piece piece = board.pieceAt(from);
        final Piece captured = board.pieceAt(to);
        final boolean isCapture = captured != null;
        boolean isNoisy = isCapture || move.isPromotion();

        if (move.equals(ttMove)) {
            // Put the TT move last; it will be tried lazily
            MoveType type = MoveType.TT_MOVE;
            final int score = -MoveType.TT_MOVE.bonus;
            return new ScoredMove(move, piece, captured, score, 0, type);
        }

        if (isNoisy) {
            return scoreNoisy(board, move, piece, captured);
        } else {
            return scoreQuiet(board, move, piece, captured, ply);
        }

    }

    protected ScoredMove scoreNoisy(Board board, Move move, Piece piece, Piece captured) {

        if (move.isPromotion()) {
            final MoveType type = move.promoPiece() == Piece.QUEEN ? MoveType.GOOD_NOISY : MoveType.BAD_NOISY;
            final int score = type.bonus;
            return new ScoredMove(move, piece, captured, score, 0, type);
        }

        int captureScore = 0;

        // Separate captures into winning and losing
        final int materialDelta = captured.value() - piece.value();
        final MoveType type = materialDelta >= 0 ? MoveType.GOOD_NOISY : MoveType.BAD_NOISY;

        captureScore += type.bonus;

        // Add MVV score to the capture score
        captureScore += MoveType.MVV_OFFSET * captured.index();

        // Tie-break with capture history
        final int historyScore = history.getCaptureHistoryTable().get(piece, move.to(), captured, board.isWhite());
        captureScore += historyScore;

        int scoreHistoryScore = history.getScoreHistoryTable().getRunningAverage(move, piece, board.isWhite());
        captureScore += scoreHistoryScore;

        return new ScoredMove(move, piece, captured, captureScore, historyScore, type);
    }

    protected ScoredMove scoreQuiet(Board board, Move move, Piece piece, Piece captured, int ply) {
        boolean white = board.isWhite();

        // Check if the move is a killer move
        int killerIndex = history.getKillerTable().getIndex(move, ply);
        int killerScore = killerIndex >= 0 ? MoveType.KILLER_OFFSET * (KillerTable.KILLERS_PER_PLY - killerIndex) : 0;

        // Get the history score for the move
        int historyScore = history.getQuietHistoryTable().get(move, piece, white);

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

        // Killers are ordered higher than normal history moves
        MoveType type = killerScore != 0 ? MoveType.KILLER : MoveType.QUIET;

        int scoreHistoryScore = history.getScoreHistoryTable().getRunningAverage(move, piece, board.isWhite());

        int score = type.bonus + killerScore + historyScore + contHistScore + scoreHistoryScore;

        return new ScoredMove(move, piece, captured, score, historyScore, type);
    }

    /**
     * Select the move with the highest score and move it to the head of the move list.
     */
    protected ScoredMove pick() {
        if (moveIndex >= moves.length) {
            return null;
        }
        int bestScore = moves[moveIndex].score();
        int bestIndex = moveIndex;
        for (int j = moveIndex + 1; j < moves.length; j++) {
            if (moves[j].score() > bestScore) {
                bestScore = moves[j].score();
                bestIndex = j;
            }
        }
        if (bestIndex != moveIndex) {
            swap(moveIndex, bestIndex);
        }
        ScoredMove scoredMove = moves[moveIndex];
        if (scoredMove == null || wasTriedLazily(scoredMove.move())) {
            return null;
        }
        return scoredMove;
    }

    protected void swap(int i, int j) {
        ScoredMove temp = moves[i];
        moves[i] = moves[j];
        moves[j] = temp;
    }

    public void setSkipQuiets(boolean skipQuiets) {
        this.skipQuiets = skipQuiets;
    }

    private boolean wasTriedLazily(Move move) {
        return move.equals(ttMove);
    }

}
