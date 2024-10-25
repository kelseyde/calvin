package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.search.PlayedMove;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.search.picker.MovePicker.Stage;

public class MoveScorer {

    private final SearchHistory history;
    private final SearchStack ss;
    private final Board board;
    private final int ply;

    public MoveScorer(Board board, SearchHistory history, SearchStack ss, int ply) {
        this.board = board;
        this.history = history;
        this.ss = ss;
        this.ply = ply;
    }

    public ScoredMove score(Move move) {
        return null;
    }

    protected ScoredMove score(Move move, Stage stage) {

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
            return scoreQuiet(board, move, piece);
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

    protected ScoredMove scoreQuiet(Board board, Move move, Piece piece) {
        boolean white = board.isWhite();
        int historyScore = history.getQuietHistoryTable().get(move, piece, white);
        int contHistScore = continuationHistoryScore(move, piece, white);
        int score = MoveType.QUIET.bonus + historyScore + contHistScore;
        return new ScoredMove(move, piece, null, score, historyScore, MoveType.QUIET);
    }

    int continuationHistoryScore(Move move, Piece piece, boolean white) {
        int contHistScore = 0;
        // Get the continuation history score for the move
        SearchStack.SearchStackEntry prevEntry = ss.get(ply - 1);
        if (prevEntry != null && prevEntry.currentMove != null) {
            PlayedMove prevMove = prevEntry.currentMove;
            contHistScore = history.getContHistTable().get(prevMove.move, prevMove.piece, move, piece, white);
        }

        SearchStack.SearchStackEntry prevEntry2 = ss.get(ply - 2);
        if (prevEntry2 != null && prevEntry2.currentMove != null) {
            PlayedMove prevMove2 = prevEntry2.currentMove;
            contHistScore += history.getContHistTable().get(prevMove2.move, prevMove2.piece, move, piece, white);
        }
        return contHistScore;
    }

}
