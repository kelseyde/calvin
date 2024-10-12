package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.movegen.MoveList;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;
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

    public Move pickNextMove() {

        Move nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE ->         pickTTMove();
                case GEN_NOISY ->       generate(MoveFilter.NOISY, Stage.NOISY);
                case NOISY ->           pickMove(Stage.GEN_QUIET);
                case GEN_QUIET ->       generate(MoveFilter.QUIET, Stage.QUIET);
                case QUIET ->           pickMove(Stage.END);
                case END ->             null;
            };
            if (stage == Stage.END) break;
        }
        return nextMove;

    }

    /**
     * Select the next move from the move list.
     * @param nextStage the next stage to move on to, if we have tried all moves in the current stage.
     */
    protected Move pickMove(Stage nextStage) {

        if (stage == Stage.QUIET && (skipQuiets || inCheck)) {
            stage = nextStage;
            return null;
        }
        if (moveIndex >= moves.length) {
            stage = nextStage;
            return null;
        }
        Move move = pick();
        moveIndex++;
        return move;

    }

    protected Move pickTTMove() {
        stage = Stage.GEN_NOISY;
        return ttMove;
    }

    protected Move generate(MoveFilter filter, Stage nextStage) {
        List<Move> stagedMoves = movegen.generateMoves(board, filter).toList();
        scoreMoves(stagedMoves);
        moveIndex = 0;
        stage = nextStage;
        return null;
    }

    protected void scoreMoves(List<Move> stagedMoves) {
        moves = new ScoredMove[stagedMoves.size()];
        for (int i = 0; i < stagedMoves.size(); i++) {
            Move move = stagedMoves.get(i);
            int score = scoreMove(board, move, ttMove, ply);
            moves[i] = new ScoredMove(move, score);
        }
    }

    protected int scoreMove(Board board, Move move, Move ttMove, int ply) {

        int from = move.from();
        int to = move.to();

        if (move.equals(ttMove)) {
            // Always put the TT move to the end - it will be tried first lazily
            return -MoveBonus.TT_MOVE_BONUS;
        }
        if (move.isPromotion()) {
            return scorePromotion(move);
        }

        Piece captured = board.pieceAt(to);
        if (captured != null) {
            return scoreCapture(board, from, to, captured);
        }
        else {
            return scoreQuiet(board, move, ply);
        }

    }

    protected int scoreCapture(Board board, int from, int to, Piece captured) {
        Piece piece = board.pieceAt(from);
        int captureScore = 0;

        // Separate captures into winning and losing
        int materialDelta = captured.value() - piece.value();
        captureScore += materialDelta >= 0 ? MoveBonus.WINNING_CAPTURE_BONUS : MoveBonus.LOSING_CAPTURE_BONUS;

        // Add MVV score to the capture score
        captureScore += MoveBonus.MVV_OFFSET * captured.index();

        // Tie-break with capture history
        captureScore += history.getCaptureHistoryTable().get(piece, to, captured, board.isWhite());

        return captureScore;
    }

    protected int scoreQuiet(Board board, Move move, int ply) {
        boolean white = board.isWhite();
        Piece piece = board.pieceAt(move.from());

        // Check if the move is a killer move
        int killerIndex = history.getKillerTable().getIndex(move, ply);
        int killerScore = killerIndex >= 0 ? MoveBonus.KILLER_OFFSET * (KillerTable.KILLERS_PER_PLY - killerIndex) : 0;

        // Get the history score for the move
        int historyScore = history.getHistoryTable().get(move, piece, white);

        // Get the continuation history score for the move
        Move prevMove = ss.getMove(ply - 1);
        Piece prevPiece = ss.getMovedPiece(ply - 1);
        int contHistScore = history.getContHistTable().get(prevMove, prevPiece, move, piece, white);

        Move prevMove2 = ss.getMove(ply - 2);
        Piece prevPiece2 = ss.getMovedPiece(ply - 2);
        contHistScore += history.getContHistTable().get(prevMove2, prevPiece2, move, piece, white);

        // Killers are ordered higher than normal history moves
        int base = 0;
        if (killerScore != 0) {
            base = MoveBonus.KILLER_MOVE_BONUS;
        } else if (historyScore != 0 || contHistScore != 0) {
            base = MoveBonus.QUIET_MOVE_BONUS;
        }

        return base + killerScore + historyScore + contHistScore;
    }

    protected int scorePromotion(Move move) {
        return move.promoPiece() == Piece.QUEEN ? MoveBonus.QUEEN_PROMO_BONUS : MoveBonus.UNDER_PROMO_BONUS;
    }

    /**
     * Select the move with the highest score and move it to the head of the move list.
     */
    protected Move pick() {
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
        Move move = moves[moveIndex].move();
        return wasTriedLazily(move) ? null : move;
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

    public record ScoredMove(Move move, int score) {}

}
