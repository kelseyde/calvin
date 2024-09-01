package com.kelseyde.calvin.search.moveordering;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
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

    static final int KILLERS_PER_PLY = 3;
    static final int MAX_KILLER_PLY = 32;
    static final int KILLER_MOVE_ORDER_BONUS = 10000;

    public static final int[][] MVV_LVA_TABLE = new int[][] {
            new int[] {15, 14, 13, 12, 11, 10},  // victim P, attacker P, N, B, R, Q, K
            new int[] {25, 24, 23, 22, 21, 20},  // victim N, attacker P, N, B, R, Q, K
            new int[] {35, 34, 33, 32, 31, 30},  // victim B, attacker P, N, B, R, Q, K
            new int[] {45, 44, 43, 42, 41, 40},  // victim R, attacker P, N, B, R, Q, K
            new int[] {55, 54, 53, 52, 51, 50},  // victim Q, attacker P, N, B, R, Q, K
    };

    Move[][] killerMoves = new Move[MAX_KILLER_PLY][KILLERS_PER_PLY];
    final int[][][] historyMoves = new int[2][64][64];

    /**
     * Orders the given list of moves based on the defined move-ordering strategy.
     *
     * @param board             The current board state.
     * @param moves             The list of moves to be ordered.
     * @param previousBestMove  The best move found at an earlier ply.
     * @param ply               The number of ply from the root node.
     * @return                  The ordered list of moves.
     */
    public List<Move> orderMoves(Board board, List<Move> moves, Move previousBestMove, int ply) {
        List<Move> orderedMoves = new ArrayList<>(moves);
        // Sort moves based on their scores in descending order
        orderedMoves.sort(Comparator.comparingInt(move -> -scoreMove(board, move, previousBestMove, ply)));
        return orderedMoves;
    }

    /**
     * Scores a move based on various heuristics such as previous best move, MVV-LVA, killer moves, and history moves.
     *
     * @param board            The current board state.
     * @param move             The move to be scored.
     * @param previousBestMove The best move found at an earlier ply.
     * @param ply              The number of ply from the root node.
     * @return                 The score of the move.
     */
    public int scoreMove(Board board, Move move, Move previousBestMove, int ply) {

        int startSquare = move.getStartSquare();
        int endSquare = move.getEndSquare();
        int moveScore = 0;

        // The previous best move from the transposition table is searched first.
        if (move.equals(previousBestMove)) {
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
            int killerScore = scoreKillerMove(move, ply);
            int historyScore = scoreHistoryMove(board, startSquare, endSquare, killerScore);
            moveScore += killerScore + historyScore;
        }

        if (move.isCastling()) {
            moveScore += CASTLING_BIAS;
        }

        return moveScore;

    }

    @Override
    public int mvvLva(Board board, Move move, Move previousBestMove) {
        if (move.equals(previousBestMove)) return PREVIOUS_BEST_MOVE_BIAS;
        int startSquare = move.getStartSquare();
        int endSquare = move.getEndSquare();
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

    private int scoreKillerMove(Move move, int ply) {
        if (ply >= MAX_KILLER_PLY) {
            return 0;
        }
        else if (move.equals(killerMoves[ply][0])) {
            return KILLER_MOVE_BIAS + (KILLER_MOVE_ORDER_BONUS * 3);
        }
        else if (move.equals(killerMoves[ply][1])) {
            return KILLER_MOVE_BIAS + (KILLER_MOVE_ORDER_BONUS * 2);
        }
        else if (move.equals(killerMoves[ply][2])) {
            return KILLER_MOVE_BIAS + (KILLER_MOVE_ORDER_BONUS);
        }
        else {
            return 0;
        }
    }

    private int scoreHistoryMove(Board board, int startSquare, int endSquare, int killerScore) {
        int colourIndex = Board.colourIndex(board.isWhiteToMove());
        int historyScore = historyMoves[colourIndex][startSquare][endSquare];
        if (killerScore == 0 && historyScore > 0) {
            historyScore += HISTORY_MOVE_BIAS;
        }
        return historyScore;
    }

    /**
     * Adds a new killer move for a given ply.
     *
     * @param ply The current ply from root.
     * @param move The new killer move to be added.
     */
    public void addKillerMove(int ply, Move move) {
        if (ply >= MAX_KILLER_PLY) {
            return;
        }
        // Check if the move already exists in the killer moves list
        for (int i = 0; i < KILLERS_PER_PLY; i++) {
            if (move.equals(killerMoves[ply][i])) {
                // Move the existing killer to the front
                for (int j = i; j > 0; j--) {
                    killerMoves[ply][j] = killerMoves[ply][j - 1];
                }
                killerMoves[ply][0] = move;
                return;
            }
        }

        // If the move is not already a killer, add it to the front and shift others
        for (int i = KILLERS_PER_PLY - 1; i > 0; i--) {
            killerMoves[ply][i] = killerMoves[ply][i - 1];
        }
        killerMoves[ply][0] = move;
    }

    /**
     * Adds a history move for a given ply and color.
     *
     * @param depth The current search depth.
     * @param historyMove The history move to be added.
     * @param white Whether the move is for white pieces.
     */
    public void incrementHistoryScore(int depth, Move historyMove, boolean white) {
        int colourIndex = Board.colourIndex(white);
        int startSquare = historyMove.getStartSquare();
        int endSquare = historyMove.getEndSquare();
        int score = depth * depth;
        historyMoves[colourIndex][startSquare][endSquare] += score;
    }

    public void decrementHistoryScore(int depth, Move historyMove, boolean white) {
        int colourIndex = Board.colourIndex(white);
        int startSquare = historyMove.getStartSquare();
        int endSquare = historyMove.getEndSquare();
        int score = depth * depth;
        historyMoves[colourIndex][startSquare][endSquare] -= score;
    }

    public void ageHistoryScores(boolean white) {
        int colourIndex = Board.colourIndex(white);
        for (int startSquare = 0; startSquare < 64; startSquare++) {
            for (int endSquare = 0; endSquare < 64; endSquare++) {
                historyMoves[colourIndex][startSquare][endSquare] /= 2;
            }
        }
    }

    public void clear() {
        killerMoves = new Move[MAX_KILLER_PLY][KILLERS_PER_PLY];
    }

}
