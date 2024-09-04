package com.kelseyde.calvin.search.moveordering;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.tables.history.ContHistTable;
import com.kelseyde.calvin.tables.history.HistoryTable;
import com.kelseyde.calvin.tables.history.KillerTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of {@link MoveOrdering} using the following move-ordering strategy:
 *  1. Previous best move found at an earlier ply
 *  2. Queen promotions
 *  3. Winning captures (sub-ordered using MVV-LVA)
 *  4. Equal captures (sub-ordered using MVV-LVA)
 *  5. Killer moves
 *  6. Losing captures (sub-ordered using MVV-LVA)
 *  7. Under-promotions
 *  8. History moves
 *  9. Everything else.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoveOrderer implements MoveOrdering {

    static final int MILLION = 1000000;
    static final int PREVIOUS_BEST_MOVE_BIAS = 10 * MILLION;
    static final int QUEEN_PROMOTION_BIAS = 9 * MILLION;
    static final int WINNING_CAPTURE_BIAS = 8 * MILLION;
    static final int EQUAL_CAPTURE_BIAS = 7 * MILLION;
    static final int KILLER_MOVE_BIAS = 6 * MILLION;
    static final int LOSING_CAPTURE_BIAS = 5 * MILLION;
    static final int HISTORY_MOVE_BIAS = 4 * MILLION;
    static final int UNDER_PROMOTION_BIAS = 3 * MILLION;
    static final int CASTLING_BIAS = 2 * MILLION;

    public static final int KILLER_MOVE_ORDER_BONUS = 10000;

    static final int MAX_HISTORY_BONUS = 1200;
    static final int MAX_HISTORY_SCORE = 8192;

    public static final int[][] MVV_LVA_TABLE = new int[][] {
            new int[] { 15, 14, 13, 12, 11, 10 },  // victim P, attacker P, N, B, R, Q, K
            new int[] { 25, 24, 23, 22, 21, 20 },  // victim N, attacker P, N, B, R, Q, K
            new int[] { 35, 34, 33, 32, 31, 30 },  // victim B, attacker P, N, B, R, Q, K
            new int[] { 45, 44, 43, 42, 41, 40 },  // victim R, attacker P, N, B, R, Q, K
            new int[] { 55, 54, 53, 52, 51, 50 },  // victim Q, attacker P, N, B, R, Q, K
    };

    final KillerTable killerTable = new KillerTable();
    final HistoryTable historyTable = new HistoryTable();
    //final ContHistTable contHistTable = new ContHistTable();

    /**
     * Orders the given list of moves based on the defined move-ordering strategy.
     *
     * @param board             The current board state.
     * @param moves             The list of moves to be ordered.
     * @param ttMove            The best move found at an earlier ply.
     * @param ply               The number of ply from the root node.
     * @return                  The ordered list of moves.
     */
    public List<Move> orderMoves(Board board, SearchStack ss, List<Move> moves, Move ttMove, int ply) {
        List<Move> orderedMoves = new ArrayList<>(moves);
        // Sort moves based on their scores in descending order
        orderedMoves.sort(Comparator.comparingInt(move -> -scoreMove(board, ss, move, ttMove, ply)));
        return orderedMoves;
    }

    /**
     * Scores a move based on various heuristics such as previous best move, MVV-LVA, killer moves, and history moves.
     *
     * @param board            The current board state.
     * @param move             The move to be scored.
     * @param ttMove           The best move found at an earlier ply.
     * @param ply              The number of ply from the root node.
     * @return                 The score of the move.
     */
    public int scoreMove(Board board, SearchStack ss, Move move, Move ttMove, int ply) {

        int startSquare = move.getFrom();
        int endSquare = move.getTo();
        int moveScore = 0;

        // The previous best move from the transposition table is searched first.
        if (move.equals(ttMove)) {
            moveScore += PREVIOUS_BEST_MOVE_BIAS;
        }

        // Then any pawn promotions
        Piece promotionPiece = move.getPromotionPiece();
        if (promotionPiece != null) {
            moveScore += scorePromotion(promotionPiece);
        }

        // Then captures, sorted by MVV-LVA
        Piece capturedPiece = board.pieceAt(endSquare);
        boolean isCapture = capturedPiece != null;
        if (isCapture) {
            moveScore += scoreCapture(board, startSquare, capturedPiece);
        }
        // Non-captures are sorted using killer score + history score
        else {
//            Piece piece = board.pieceAt(startSquare);
//            Move prevMove = ss.getMove(ply - 1);
//            Piece prevPiece = ss.getMovedPiece(ply - 1);

            int killerScore = killerTable.score(move, ply, KILLER_MOVE_BIAS, KILLER_MOVE_ORDER_BONUS);
            int historyScore = historyTable.get(move, board.isWhiteToMove());
            //int contHistScore = contHistTable.get(prevMove, prevPiece, move, piece, board.isWhiteToMove());
            //int historyBase = killerScore == 0 && (historyScore > 0 || contHistScore > 0) ? HISTORY_MOVE_BIAS : 0;
            int historyBase = killerScore == 0 && historyScore > 0 ? HISTORY_MOVE_BIAS : 0;

            moveScore += killerScore + historyBase + historyScore;
        }

        if (move.isCastling()) {
            moveScore += CASTLING_BIAS;
        }

        return moveScore;

    }

    @Override
    public int mvvLva(Board board, Move move, Move previousBestMove) {
        if (move.equals(previousBestMove)) return PREVIOUS_BEST_MOVE_BIAS;
        int startSquare = move.getFrom();
        int endSquare = move.getTo();
        Piece capturedPiece = board.pieceAt(endSquare);
        if (capturedPiece == null) return 0;
        Piece piece = board.pieceAt(startSquare);
        return MVV_LVA_TABLE[capturedPiece.getIndex()][piece.getIndex()];
    }

    private int scorePromotion(Piece promotionPiece) {
        return Piece.QUEEN == promotionPiece ? QUEEN_PROMOTION_BIAS : UNDER_PROMOTION_BIAS;
    }

    private int scoreCapture(Board board, int startSquare, Piece capturedPiece) {
        Piece piece = board.pieceAt(startSquare);
        int captureScore = 0;
        captureScore += MVV_LVA_TABLE[capturedPiece.getIndex()][piece.getIndex()];
        int materialDelta = capturedPiece.getValue() - piece.getValue();
        if (materialDelta > 0) {
            captureScore += WINNING_CAPTURE_BIAS;
        } else if (materialDelta == 0) {
            captureScore += EQUAL_CAPTURE_BIAS;
        } else {
            captureScore += LOSING_CAPTURE_BIAS;
        }
        return captureScore;
    }

    /**
     * Adds a new killer move for a given ply.
     *
     * @param ply The current ply from root.
     * @param move The new killer move to be added.
     */
    public void addKillerMove(int ply, Move move) {
        killerTable.add(ply, move);
    }

    /**
     * Adds a history move for a given ply and color.
     *
     * @param historyMove The history move to be added.
     * @param ss          The search stack.
     * @param depth       The current search depth.
     * @param ply
     * @param white       Whether the move is for white pieces.
     */
    public void addHistoryScore(Move historyMove, SearchStack ss, int depth, int ply, boolean white) {
        historyTable.add(depth, historyMove, white);
//        Piece currentPiece = ss.getMovedPiece(ply);
//        Move prevMove = ss.getMove(ply - 1);
//        Piece prevPiece = ss.getMovedPiece(ply - 1);
//        contHistTable.add(prevMove, prevPiece, historyMove, currentPiece, depth, white);
    }

    public void subHistoryScore(Move historyMove, SearchStack ss, int depth, int ply, boolean white) {
        historyTable.sub(depth, historyMove, white);
//        Move currentMove = ss.getMove(ply);
//        Piece currentPiece = ss.getMovedPiece(ply);
//        Move prevMove = ss.getMove(ply - 1);
//        Piece prevPiece = ss.getMovedPiece(ply - 1);
//        contHistTable.sub(prevMove, prevPiece, currentMove, currentPiece, depth, white);
    }

    public void ageHistoryScores(boolean white) {
        historyTable.ageScores(white);
    }

    public void clear() {
        killerTable.clear();
    }

}
