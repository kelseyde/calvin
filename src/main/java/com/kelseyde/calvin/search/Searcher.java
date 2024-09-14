package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.evaluation.SEE;
import com.kelseyde.calvin.evaluation.Score;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGeneration.MoveFilter;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.SearchStack.PlayedMove;
import com.kelseyde.calvin.search.picker.MovePicker;
import com.kelseyde.calvin.search.picker.QuiescentMovePicker;
import com.kelseyde.calvin.tables.tt.HashEntry;
import com.kelseyde.calvin.tables.tt.HashFlag;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Classical alpha-beta search with iterative deepening. This is the main search algorithm used by the engine.
 * </p>
 * Alpha-beta search seeks to reduce the number of nodes that need to be evaluated in the search tree. It does this by
 * pruning branches that are guaranteed to be worse than the best move found so far, or that are guaranteed to be 'too
 * good' and could only be reached by sup-optimal play by the opponent.
 * @see <a href="https://www.chessprogramming.org/Alpha-Beta">Chess Programming Wiki</a>
 * </p>
 * Iterative deepening is a search strategy that does a full search at a depth of 1 ply, then a full search at 2 ply,
 * then 3 ply and so on, until the time limit is exhausted. In case the timeout is reached in the middle of an iteration,
 * the search can still fall back on the best move found in the previous iteration. By prioritising searching the best
 * move found in the previous iteration -- and by using a {@link TranspositionTable} -- the iterative approach is much
 * more efficient than it might sound.
 * @see <a href="https://www.chessprogramming.org/Iterative_Deepening">Chess Programming Wiki</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Searcher implements Search {

    final EngineConfig config;
    final ThreadManager threadManager;
    final MoveGeneration moveGenerator;
    final Evaluation evaluator;
    final SEE see;
    final SearchStack ss;
    final SearchHistory history;
    final TranspositionTable tt;

    Board board;
    Instant start;
    TimeControl tc;
    int currentDepth;
    int nodes;

    public Searcher(EngineConfig config, ThreadManager threadManager, TranspositionTable tt) {
        this.config = config;
        this.threadManager = threadManager;
        this.tt = tt;
        this.moveGenerator = new MoveGenerator();
        this.evaluator = new NNUE();
        this.see = new SEE();
        this.ss = new SearchStack();
        this.history = new SearchHistory();
    }

    /**
     * Search the current position, increasing the depth each iteration, to find the best move within the given time limit.
     * @param timeControl the maximum duration to search
     * @return a {@link SearchResult} containing the best move, the eval, and other search info.
     */
    @Override
    public SearchResult search(TimeControl timeControl) {

        start = Instant.now();

        List<Move> rootMoves = moveGenerator.generateMoves(board);
        if (rootMoves.size() == 1) {
            return handleOnlyOneLegalMove(rootMoves);
        }

        tc = timeControl;
        ss.clear();
        nodes = 0;
        currentDepth = 1;
        Move bestMoveRoot = null;
        int bestScoreRoot = 0;
        history.getHistoryTable().ageScores(board.isWhiteToMove());

        int alpha = Score.MIN;
        int beta = Score.MAX;

        int retries = 0;
        int reduction = 0;
        int maxReduction = config.getAspMaxReduction();
        int margin = config.getAspMargin();
        int failMargin = config.getAspFailMargin();

        while (!shouldStopSoft() && currentDepth < Search.MAX_DEPTH) {
            // Reset variables for the current depth iteration
            int searchDepth = currentDepth - reduction;
            int delta = failMargin * retries;

            // Perform alpha-beta search for the current depth
            int score = search(searchDepth, 0, alpha, beta);

            // Update the best move and evaluation if a better move is found
            Move move = ss.getBestMove(0).getMove();
            history.updateBestMoveStability(bestMoveRoot, move);
            history.updateBestScoreStability(bestScoreRoot, score);

            if (move != null) {
                bestMoveRoot = move;
                bestScoreRoot = score;
                threadManager.handleSearchResult(SearchResult.of(bestMoveRoot, bestScoreRoot, currentDepth, start, nodes));
            }

            // Check if search is cancelled or a checkmate is found
            if (shouldStop() || Score.isMateScore(score)) {
                break;
            }

            // Aspiration windows - https://www.chessprogramming.org/Aspiration_Windows
            // Use the search score from the previous iteration to guess the score from the current iteration.
            // Based on this guess, we can narrow the alpha-beta window around the previous score, causing more cut-offs
            // and thus speeding up the search. If the true score is outside the window, a costly re-search is required.

            // Adjust the aspiration window in case the score fell outside the current window
            if (score <= alpha) {
                // If score <= alpha, re-search with an expanded aspiration window
                reduction = 0;
                retries++;
                alpha -= delta;
                continue;
            }
            if (score >= beta) {
                // If score >= beta, re-search with an expanded aspiration window
                reduction = Math.min(maxReduction, reduction + 1);
                retries++;
                beta += delta;
                continue;
            }

            // Center the aspiration window around the score from the current iteration, to be used next time.
            alpha = score - margin;
            beta = score + margin;

            // Increment depth and reset retry counter for next iteration
            retries = 0;
            currentDepth++;
        }

        // Clear move ordering cache and return the search result
        history.getKillerTable().clear();

        return SearchResult.of(bestMoveRoot, bestScoreRoot, currentDepth, start, nodes);

    }

    /**
     * Run a single iteration of the iterative deepening search for a specific depth.
     *
     * @param depth               The number of ply deeper left to go in the current search ('ply remaining').
     * @param ply                 The number of ply already examined in the current search ('ply from root').
     * @param alpha               The lower bound for child nodes at the current search depth.
     * @param beta                The upper bound for child nodes at the current search depth.
     */
    public int search(int depth, int ply, int alpha, int beta) {

        // If timeout is reached, exit immediately
        if (shouldStop()) return alpha;

        // If depth is reached, drop into quiescence search
        if (depth <= 0) return quiescenceSearch(alpha, beta, 1, ply);

        // If the game is drawn by repetition, insufficient material or fifty move rule, return zero
        if (ply > 0 && isDraw()) return Score.DRAW;

        boolean rootNode = ply == 0;
        boolean pvNode = beta - alpha > 1;

        // Mate Distance Pruning - https://www.chessprogramming.org/Mate_Distance_Pruning
        // Exit early if we have already found a forced mate at an earlier ply
        alpha = Math.max(alpha, -Score.MATE + ply);
        beta = Math.min(beta, Score.MATE - ply);
        if (alpha >= beta) return alpha;

        history.getKillerTable().clear(ply + 1);

        // Probe the transposition table in case this node has been searched before. If so, we can potentially re-use the
        // result of the previous search and save some time, only if the following conditions are met:
        //  a) we are not in a PV node,
        //  b) it was searched to a sufficient depth, and
        //  c) the score is either exact, or outside the bounds of the current alpha-beta window.
        HashEntry ttEntry = tt.get(board.key(), ply);
        if (!pvNode
                && ttEntry != null
                && ttEntry.isSufficientDepth(depth)
                && ttEntry.isWithinBounds(alpha, beta)) {
            return ttEntry.getScore();
        }

        Move ttMove = null;
        if (ttEntry != null && ttEntry.getMove() != null) {
            // Even if we can't re-use the entire tt entry, we can still use the stored move to improve move ordering.
            ttMove = ttEntry.getMove();
        }

        MovePicker movePicker = new MovePicker(moveGenerator, ss, history, board, ply);
        movePicker.setTtMove(ttMove);

        boolean inCheck = moveGenerator.isCheck(board, board.isWhiteToMove());
        movePicker.setInCheck(inCheck);

        // Check extension - https://www.chessprogramming.org/Check_Extension
        // If we are in check then there if a forcing sequence, so we could benefit from searching one ply deeper to
        // retrieve a more accurate evaluation. We can skip depth == 1 checks as they are already handled by quiescence.
        if (inCheck && depth > 1) {
            depth++;
        }

        // Internal Iterative Deepening - https://www.chessprogramming.org/Internal_Iterative_Deepening
        // If the position has not been searched yet, the search will be potentially expensive. So let's search with a
        // reduced depth expecting to record a move that we can use later for a full-depth search.
        if (!rootNode
                && !inCheck
                && (ttEntry == null || ttEntry.getMove() == null)
                && ply > 0
                && depth >= config.getIirDepth()) {
            --depth;
        }

        // Re-use cached static eval if available. Don't compute static eval while in check.
        int staticEval = Integer.MIN_VALUE;
        if (!inCheck) {
            staticEval = ttEntry != null ? ttEntry.getStaticEval() : evaluator.evaluate();
        }

        ss.setStaticEval(ply, staticEval);

        // We are 'improving' if the static eval of the current position is greater than it was on our previous turn.
        // If our position is improving we can be more aggressive in our beta pruning - where the eval is too high - but
        // should be more cautious in our alpha pruning - where the eval is too low.
        boolean improving = isImproving(ply, staticEval);

        // Pre-move-loop pruning: If the static eval indicates a fail-high or fail-low, there are several heuristic we
        // can employ to prune the node and its entire subtree, without searching any moves.
        if (!pvNode && !inCheck) {

            // Reverse Futility Pruning - https://www.chessprogramming.org/Reverse_Futility_Pruning
            // If the static evaluation + some significant margin is still above beta, then let's assume this position
            // is a cut-node and will fail-high, and not search any further.
            if (depth <= config.getRfpDepth()
                && staticEval - depth * config.getRfpMargin()[improving ? 1 : 0] >= beta
                && !Score.isMateScore(alpha)) {
                return staticEval;
            }

            // Null Move Pruning - https://www.chessprogramming.org/Null_Move_Pruning
            // If the static evaluation + some significant margin is still above beta after giving the opponent two moves
            // in a row (making a 'null' move), then let's assume this position is a cut-node and will fail-high, and
            // not search any further.
            if (ss.isNullMoveAllowed(ply)
                && depth >= config.getNmpDepth()
                && staticEval >= beta - (config.getNmpMargin() * (improving ? 1 : 0))
                && board.hasPiecesRemaining(board.isWhiteToMove())) {

                ss.setNullMoveAllowed(ply + 1, false);
                board.makeNullMove();

                int r = 3 + depth / 3;
                int score = -search(depth - r, ply + 1, -beta, -beta + 1);

                board.unmakeNullMove();
                ss.setNullMoveAllowed(ply + 1, true);

                if (score >= beta) {
                    tt.put(board.key(), HashFlag.LOWER, depth, ply, ttMove, staticEval, beta);
                    return Score.isMateScore(score) ? beta : score;
                }
            }

        }

        Move bestMove = null;
        int bestScore = Score.MIN;
        HashFlag flag = HashFlag.UPPER;

        int movesSearched = 0;
        List<Move> quietsSearched = new ArrayList<>();
        List<PlayedMove> capturesSearched = new ArrayList<>();

        while (true) {

            Move move = movePicker.pickNextMove();
            if (move == null) break;
            //if (bestMove == null) bestMove = move;
            movesSearched++;

            Piece piece = board.pieceAt(move.getFrom());
            Piece capturedPiece = board.pieceAt(move.getTo());
            boolean isCapture = capturedPiece != null;
            boolean isPromotion = move.getPromotionPiece() != null;

            // Futility Pruning - https://www.chessprogramming.org/Futility_Pruning
            // If the static evaluation + some margin is still < alpha, and the current move is not interesting (checks,
            // captures, promotions), then let's assume it will fail low and prune this node.
            if (!pvNode
                && depth <= config.getFpDepth()
                && staticEval + config.getFpMargin()[depth] < alpha
                && !inCheck
                && !isCapture
                && !isPromotion) {
                movePicker.setSkipQuiets(true);
                continue;
            }

            evaluator.makeMove(board, move);
            if (!board.makeMove(move)) continue;
            nodes++;

            boolean isCheck = moveGenerator.isCheck(board, board.isWhiteToMove());
            boolean isQuiet = !isCheck && !isCapture && !isPromotion;
            ss.setMove(ply, move, piece, capturedPiece, isCapture, isQuiet);
            if (isQuiet) {
                quietsSearched.add(move);
            } else if (isCapture) {
                capturesSearched.add(new PlayedMove(move, piece, capturedPiece, true, false));
            }

            // Late Move Pruning - https://www.chessprogramming.org/Futility_Pruning#Move_Count_Based_Pruning
            // If the move is ordered very late in the list, and isn't a 'noisy' move like a check, capture or
            // promotion, let's assume it's less likely to be good, and fully skip searching that move.
            int lmpCutoff = (depth * config.getLmpMultiplier()) / (1 + (improving ? 0 : 1));
            if (!pvNode
                && !inCheck
                && isQuiet
                && depth <= config.getLmpDepth()
                && movesSearched >= lmpCutoff) {
                evaluator.unmakeMove();
                board.unmakeMove();
                ss.unsetMove(ply);
                movePicker.setSkipQuiets(true);
                continue;
            }

            int score;
            if (isDraw()) {
                // No need to search if the position is a legal draw (3-fold, insufficient material, or 50-move rule).
                score = Score.DRAW;
            }
            else if (pvNode && movesSearched == 1) {
                // Principal Variation Search - https://www.chessprogramming.org/Principal_Variation_Search
                // The first move must be searched with the full alpha-beta window. If our move ordering is any good
                // then we expect this to be the best move, and so we need to retrieve the exact score.
                score = -search(depth - 1, ply + 1, -beta, -alpha);
            }
            else {
                // Late Move Reductions - https://www.chessprogramming.org/Late_Move_Reductions
                // If the move is ordered late in the list, and isn't a 'noisy' move like a check, capture or promotion,
                // let's save time by assuming it's less likely to be good, and reduce the search depth.
                int reduction = 0;
                if (depth >= config.getLmrDepth()
                    && movesSearched >= (pvNode ? config.getLmrMinSearchedMoves() + 1 : config.getLmrMinSearchedMoves() - 1)
                    && isQuiet) {
                    reduction = config.getLmrReductions()[depth][movesSearched];
                    if (pvNode) {
                        reduction--;
                    }
                    if (ttEntry != null && ttEntry.getMove() != null && isCapture) {
                        reduction++;
                    }
                }

                // For all other moves apart from the principal variation, search with a null window (-alpha - 1, -alpha),
                // to try and prove the move will fail low while saving the time spent on a full search.
                score = -search(depth - 1 - reduction, ply + 1, -alpha - 1, -alpha);

                if (score > alpha && (score < beta || reduction > 0)) {
                    // If we reduced the depth and/or used a null window, and the score beat alpha, we need to do a
                    // re-search with the full window and depth. This is costly, but hopefully doesn't happen too often.
                    score = -search(depth - 1, ply + 1, -beta, -alpha);
                }
            }

            evaluator.unmakeMove();
            board.unmakeMove();
            ss.unsetMove(ply);

            if (shouldStop()) {
                return alpha;
            }

            if (score > bestScore) {
                bestScore = score;
            }

            if (score > alpha) {
                // If the score is better than alpha, we have a new best move.
                bestMove = move;
                ss.setBestMove(ply, move, piece, capturedPiece, isCapture, isQuiet);

                alpha = score;
                flag = HashFlag.EXACT;

                if (score >= beta) {
                    // If the score is greater than beta, the position is outside the bounds of the current alpha-beta
                    // window. Our opponent won't allow us to reach this position, so we can cut off the search here.
                    flag = HashFlag.LOWER;
                    break;
                }
            }
        }

        if (movesSearched == 0) {
            // If there are no legal moves, and it's check, then it's checkmate. Otherwise, it's stalemate.
            return inCheck ? -Score.MATE + ply : Score.DRAW;
        }

        if (bestMove != null) {
            PlayedMove best = ss.getBestMove(ply);
            if (best.isQuiet()) {
                updateQuietHistory(best, depth, ply, quietsSearched, capturesSearched);
            }
            else if (best.isCapture()) {
                updateCaptureHistory(best, depth, capturesSearched);
            }
        }

        // Store the best move and score in the transposition table for future reference.
        tt.put(board.key(), flag, depth, ply, bestMove, staticEval, bestScore);

        return bestScore;

    }

    /**
     * Extend the search by searching captures until a 'quiet' position is reached, where there are no further captures
     * and therefore limited potential for winning tactics that drastically alter the evaluation. Used to mitigate the
     * worst of the 'horizon effect'.
     *
     * @see <a href="https://www.chessprogramming.org/Quiescence_Search">Chess Programming Wiki</a>
     */
    int quiescenceSearch(int alpha, int beta, int depth, int ply) {
        if (shouldStop()) {
            return alpha;
        }

        QuiescentMovePicker movePicker = new QuiescentMovePicker(moveGenerator, ss, history, board, ply);

        // Exit the quiescence search early if we already have an accurate score stored in the hash table.
        HashEntry ttEntry = tt.get(board.key(), ply);
        if (ttEntry != null
                && ttEntry.isSufficientDepth(depth)
                && ttEntry.isWithinBounds(alpha, beta)) {
            return ttEntry.getScore();
        }
        if (ttEntry != null && ttEntry.getMove() != null) {
            movePicker.setTtMove(ttEntry.getMove());
        }

        boolean isInCheck = moveGenerator.isCheck(board, board.isWhiteToMove());

        // Re-use cached static eval if available. Don't compute static eval while in check.
        int staticEval = Integer.MIN_VALUE;
        if (!isInCheck) {
            staticEval = ttEntry != null ? ttEntry.getStaticEval() : evaluator.evaluate();
        }

        if (isInCheck) {
            // If we are in check, we need to generate 'all' legal moves that evade check, not just captures. Otherwise,
            // we risk missing simple mate threats.
            movePicker.setFilter(MoveFilter.ALL);
        } else {
            // If we are not in check, then we have the option to 'stand pat', i.e. decline to continue the capture chain,
            // if the static evaluation of the position is good enough.
            if (staticEval >= beta) {
                return staticEval;
            }
            if (staticEval > alpha) {
                alpha = staticEval;
            }
            MoveFilter filter = depth == 1 ? MoveFilter.NOISY : MoveFilter.CAPTURES_ONLY;
            movePicker.setFilter(filter);
        }

        int movesSearched = 0;

        int bestScore = alpha;

        while (true) {

            Move move = movePicker.pickNextMove();
            if (move == null) break;
            movesSearched++;

            if (!isInCheck) {
                // Delta Pruning - https://www.chessprogramming.org/Delta_Pruning
                // If the captured piece + a margin still has no potential of raising alpha, let's assume this position
                // is bad for us no matter what we do, and not bother searching any further
                Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(move.getTo());
                if (capturedPiece != null
                        && !move.isPromotion()
                        && (staticEval + capturedPiece.getValue() + config.getDpMargin() < alpha)) {
                    continue;
                }
                // Static Exchange Evaluation - https://www.chessprogramming.org/Static_Exchange_Evaluation
                // Evaluate the possible captures + recaptures on the target square, in order to filter out losing capture
                // chains, such as capturing with the queen a pawn defended by another pawn.
                int seeScore = see.evaluate(board, move);
                if ((depth <= 3 && seeScore < 0)
                        || (depth > 3 && seeScore <= 0)) {
                    continue;
                }
            }

            evaluator.makeMove(board, move);
            if (!board.makeMove(move)) continue;
            nodes++;
            int score = isDraw() ? Score.DRAW : -quiescenceSearch(-beta, -alpha, depth + 1, ply + 1);
            evaluator.unmakeMove();
            board.unmakeMove();

            if (score > bestScore) {
                bestScore = score;
            }
            if (score >= beta) {
                return score;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        if (movesSearched == 0 && isInCheck) {
            return -Score.MATE + ply;
        }

        return bestScore;

    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        this.evaluator.setPosition(board);
    }

    @Override
    public void setHashSize(int hashSizeMb) {
        this.tt.resize(hashSizeMb);
    }

    @Override
    public void setThreadCount(int threadCount) {
        // do nothing as this implementation is single-threaded
    }

    private void updateQuietHistory(PlayedMove bestMove, int depth, int ply, List<Move> quietsSearched, List<PlayedMove> capturesSearched) {

        history.getKillerTable().add(ply, bestMove.getMove());

        for (Move quiet : quietsSearched) {
            boolean good = bestMove.getMove().equals(quiet);
            history.getHistoryTable().update(quiet, board.isWhiteToMove(), good);
        }

        for (PlayedMove captureMove : capturesSearched) {
            boolean good = bestMove.equals(captureMove);
            Piece piece = captureMove.getPiece();
            int to = captureMove.getMove().getTo();
            Piece captured = captureMove.getCaptured();
            history.getCaptureHistoryTable().update(piece, to, captured, board.isWhiteToMove(), good);
        }

    }

    private void updateCaptureHistory(PlayedMove bestMove, int depth, List<PlayedMove> capturesSearched) {
        for (PlayedMove capture : capturesSearched) {
            boolean good = bestMove.equals(capture);
            Piece piece = capture.getPiece();
            int to = capture.getMove().getTo();
            Piece captured = capture.getCaptured();
            history.getCaptureHistoryTable().update(piece, to, captured, board.isWhiteToMove(), good);
        }
    }

    private boolean shouldStop() {
        // Exit if global search is cancelled
        if (config.isSearchCancelled()) return true;
        return !config.isPondering() && tc != null && tc.isHardLimitReached(start, currentDepth, nodes);
    }

    private boolean shouldStopSoft() {
        if (config.isPondering() || tc == null)
            return false;
        int bestMoveStability = history.getBestMoveStability();
        int scoreStability = history.getBestScoreStability();
        return tc.isSoftLimitReached(start, currentDepth, nodes, bestMoveStability, scoreStability);
    }

    private boolean isDraw() {
        return Score.isEffectiveDraw(board);
    }

    /**
     * Compute whether our position is improving relative to previous static evaluations. If we are in check, we're not
     * improving. If we were in check 2 plies ago, check 4 plies ago. If we were in check 4 plies ago, return true.
     */
    private boolean isImproving(int ply, int staticEval) {
        if (staticEval == Integer.MIN_VALUE) return false;
        if (ply < 2) return false;
        int lastEval = ss.getStaticEval(ply - 2);
        if (lastEval == Integer.MIN_VALUE) {
            if (ply < 4) return false;
            lastEval = ss.getStaticEval(ply - 4);
            if (lastEval == Integer.MIN_VALUE) {
                return true;
            }
        }
        return lastEval < staticEval;
    }

    private SearchResult handleOnlyOneLegalMove(List<Move> rootMoves) {
        // If there is only one legal move, play it immediately
        Move move = rootMoves.get(0);
        int eval = evaluator.evaluate();
        SearchResult result = SearchResult.of(move, eval, 1, start, 1);
        threadManager.handleSearchResult(result);
        return result;
    }

    @Override
    public TranspositionTable getTranspositionTable() {
        return tt;
    }

    @Override
    public void clearHistory() {
        tt.clear();
        evaluator.clearHistory();
        history.clear();
    }

}
