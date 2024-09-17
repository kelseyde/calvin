package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.search.SearchStack.PlayedMove;
import com.kelseyde.calvin.tables.history.CaptureHistoryTable;
import com.kelseyde.calvin.tables.history.ContinuationHistoryTable;
import com.kelseyde.calvin.tables.history.HistoryTable;
import com.kelseyde.calvin.tables.history.KillerTable;
import lombok.Data;

import java.util.List;

@Data
public class SearchHistory {

    private static final int[] CONT_HIST_PLIES = { 1, 2 };

    private KillerTable killerTable = new KillerTable();
    private HistoryTable historyTable = new HistoryTable();
    private ContinuationHistoryTable contHistTable = new ContinuationHistoryTable();
    private CaptureHistoryTable captureHistoryTable = new CaptureHistoryTable();

    private int bestMoveStability = 0;
    private int bestScoreStability = 0;

    public void updateHistory(PlayedMove bestMove, boolean white, int depth, int ply, int score, int beta, SearchStack ss, List<PlayedMove> quiets, List<PlayedMove> captures) {

        boolean failHigh = score >= beta;
        if (failHigh) {
            killerTable.add(ply, bestMove.getMove());
        }

        if (bestMove.isQuiet()) {
            for (PlayedMove quiet : quiets) {
                boolean good = bestMove.getMove().equals(quiet.getMove());
                historyTable.update(quiet.getMove(), depth, white, good);

                for (int prevPly : CONT_HIST_PLIES) {
                    Move prevMove = ss.getMove(ply - prevPly);
                    Piece prevPiece = ss.getMovedPiece(ply - prevPly);
                    contHistTable.update(prevMove, prevPiece, quiet.getMove(), quiet.getPiece(), depth, white, good);
                }
            }
        }

        for (PlayedMove capture : captures) {
            boolean good = bestMove.equals(capture);
            Piece piece = capture.getPiece();
            int to = capture.getMove().to();
            Piece captured = capture.getCaptured();
            captureHistoryTable.update(piece, to, captured, depth, white, good);
        }

    }

    public void updateQuietHistory(
            PlayedMove bestMove, boolean white, int depth, int ply, SearchStack ss, List<PlayedMove> quietsSearched, List<PlayedMove> capturesSearched, boolean failHigh) {

        if (failHigh) {
            killerTable.add(ply, bestMove.getMove());
        }

        for (PlayedMove quiet : quietsSearched) {
            boolean good = bestMove.getMove().equals(quiet.getMove());
            historyTable.update(quiet.getMove(), depth, white, good);

            Move prevMove = ss.getMove(ply - 1);
            Piece prevPiece = ss.getMovedPiece(ply - 1);
            contHistTable.update(prevMove, prevPiece, quiet.getMove(), quiet.getPiece(), depth, white, good);
        }

        for (PlayedMove capture : capturesSearched) {
            boolean good = bestMove.equals(capture);
            Piece piece = capture.getPiece();
            int to = capture.getMove().to();
            Piece captured = capture.getCaptured();
            captureHistoryTable.update(piece, to, captured, depth, white, good);
        }

    }

    public void updateCaptureHistory(PlayedMove bestMove, boolean white, int depth, List<PlayedMove> capturesSearched) {
        for (PlayedMove capture : capturesSearched) {
            boolean good = bestMove.equals(capture);
            Piece piece = capture.getPiece();
            int to = capture.getMove().to();
            Piece captured = capture.getCaptured();
            captureHistoryTable.update(piece, to, captured, depth, white, good);
        }
    }

    public void updateBestMoveStability(Move bestMovePrevious, Move bestMoveCurrent) {
        if (bestMovePrevious == null || bestMoveCurrent == null) {
            return;
        }
        bestMoveStability = bestMovePrevious.equals(bestMoveCurrent) ? bestMoveStability + 1 : 0;
    }

    public void updateBestScoreStability(int scorePrevious, int scoreCurrent) {
        bestScoreStability = scoreCurrent >= scorePrevious - 10 && scoreCurrent <= scorePrevious + 10 ? bestScoreStability + 1 : 0;
    }

    public void reset() {
        bestMoveStability = 0;
        bestScoreStability = 0;
        historyTable.ageScores(true);
        historyTable.ageScores(false);
    }

    public void clear() {
        killerTable.clear();
        historyTable.clear();
        contHistTable.clear();
        captureHistoryTable.clear();
    }

}
