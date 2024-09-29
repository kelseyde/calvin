package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.movegen.MoveGenerator.MoveFilter;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.tables.history.KillerTable;
import com.kelseyde.calvin.utils.notation.FEN;

import java.util.Arrays;
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
        GOOD_NOISY,
        KILLER,
        BAD_NOISY,
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

    boolean tryKillers;
    int killerIndex;
    Move[] killers;

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
        this.tryKillers = !inCheck;
        this.stage = ttMove != null ? Stage.TT_MOVE : Stage.GEN_NOISY;
    }

    public Move pickNextMove() {

        Move nextMove = null;
        while (nextMove == null) {
            nextMove = switch (stage) {
                case TT_MOVE ->      pickTTMove();
                case GEN_NOISY ->    generate(MoveFilter.NOISY, Stage.GOOD_NOISY);
                case GOOD_NOISY ->   pickMove(Stage.KILLER);
                case KILLER ->       pickKiller();
                case BAD_NOISY ->    pickMove(Stage.GEN_QUIET);
                case GEN_QUIET ->    generate(MoveFilter.QUIET, Stage.QUIET);
                case QUIET ->        pickMove(Stage.END);
                case END ->          null;
            };
            if (stage == Stage.END) break;
        }
//        System.out.println("Move: " + (nextMove != null ? Move.toUCI(nextMove) : null));
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


        ScoredMove move = pick();
        if (tryKillers && stage == Stage.GOOD_NOISY && move != null && move.getStage() == Stage.BAD_NOISY) {
            stage = Stage.KILLER;
            //System.out.println("stage: " + stage);
            return null;
        }

        moveIndex++;
//        System.out.println("    Pick move: " + (move != null ? Move.toUCI(move.move()): null));
        return move != null ? move.move() : null;

    }

    protected Move pickKiller() {
        if (!tryKillers || skipQuiets) {
            stage = Stage.BAD_NOISY;
            return null;
        }
        if (killers == null) {
            killers = history.getKillerTable().getKillers(ply);
            killerIndex = 0;
        }
        if (killerIndex >= killers.length) {
            stage = Stage.BAD_NOISY;
            return null;
        }
        while (killerIndex < killers.length) {
            Move killer = killers[killerIndex++];
            if (killer == null) {
                continue;
            }
            if (killer.equals(ttMove)) {
                continue;
            }
            if (!movegen.isLegal(board, killer)) {
                continue;
            }
            if (board.pieceAt(killer.to()) != null) {
                // special case where the killer move was quiet, but in the
                // current position it is a capture, and so has already been tried
                continue;
            }
//            System.out.println("    Pick killer: " + Move.toUCI(killer));

            List<Move> legalMoves = movegen.generateMoves(board);
            if (!legalMoves.contains(killer)) {
                System.out.println("fen: " + FEN.toFEN(board));
                System.out.println("move: " + Move.toUCI(killer));
            }

            return killer;
        }
        stage = Stage.BAD_NOISY;
        //System.out.println("stage: " + stage);
        return null;
    }

    protected Move pickTTMove() {
        stage = Stage.GEN_NOISY;
        return ttMove;
    }

    protected Move generate(MoveFilter filter, Stage nextStage) {
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
        if (killerScore != 0) {
            return -MoveBonus.TT_MOVE_BONUS;
        }

        // Get the history score for the move
        int historyScore = history.getHistoryTable().get(move, white);

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
        ScoredMove move = moves[moveIndex];
        return wasTriedLazily(move.move) ? null : move;
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
//        System.out.println("        Checking if move was tried lazily: " + Move.toUCI(move));
        if (move.equals(ttMove)) {
//            System.out.println("        TT move was tried lazily: " + Move.toUCI(move));
            return true;
        }
        if (killers != null) {
//            System.out.println("        Checking killers: " + Arrays.stream(killers).map(Move::toUCI).toList());
            for (Move killer : killers) {
                if (move.equals(killer)) {
//                    System.out.println("        Killer move was tried lazily: " + Move.toUCI(move));
                    return true;
                }
            }
        }
//        System.out.println("        Move was not tried lazily: " + Move.toUCI(move));
        return false;
    }

    public record ScoredMove(Move move, int score) {

        public Stage getStage() {
            if (score < MoveBonus.TT_MOVE_BONUS && score >= MoveBonus.WINNING_CAPTURE_BONUS) {
                return Stage.GOOD_NOISY;
            }
            else if (score < MoveBonus.WINNING_CAPTURE_BONUS && score >= MoveBonus.KILLER_MOVE_BONUS) {
                return Stage.KILLER;
            }
            else if (score < MoveBonus.KILLER_MOVE_BONUS && score >= MoveBonus.LOSING_CAPTURE_BONUS) {
                return Stage.BAD_NOISY;
            }
            else {
                return Stage.QUIET;
            }

        }

    }

}
