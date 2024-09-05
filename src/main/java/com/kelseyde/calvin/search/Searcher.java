package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.Score;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGeneration.MoveFilter;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.search.moveordering.SEE;
import com.kelseyde.calvin.search.moveordering.see.SEEAfterMove;
import com.kelseyde.calvin.search.picker.MovePicker;
import com.kelseyde.calvin.search.picker.QuiescentMovePicker;
import com.kelseyde.calvin.tables.tt.HashEntry;
import com.kelseyde.calvin.tables.tt.HashFlag;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
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
 * move found in the previous iteration, as well as the other ordering heuristics in the {@link MoveOrderer} -- and by
 * using a {@link TranspositionTable} -- the iterative approach is much more efficient than it might sound.
 * @see <a href="https://www.chessprogramming.org/Iterative_Deepening">Chess Programming Wiki</a>
 */
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Searcher implements Search {

    EngineConfig config;
    ThreadManager threadManager;
    MoveGeneration moveGenerator;
    MoveOrdering moveOrderer;
    Evaluation evaluator;
    TranspositionTable transpositionTable;

    Board board;
    int nodes;
    Instant start;
    TimeControl tc;
    SearchStack ss = new SearchStack();
    boolean cancelled;

    int currentDepth;

    Move bestMoveRoot;
    Move bestMoveCurrentDepth;
    int bestMoveStability;

    int bestScoreRoot;
    int bestScoreCurrentDepth;
    int previousEval;
    int evalStability;

    SearchResult result;

    public Searcher(EngineConfig config,
                    ThreadManager threadManager,
                    MoveGeneration moveGenerator,
                    MoveOrdering moveOrderer,
                    Evaluation evaluator,
                    TranspositionTable transpositionTable) {
        this.config = config;
        this.threadManager = threadManager;
        this.moveGenerator = moveGenerator;
        this.moveOrderer = moveOrderer;
        this.evaluator = evaluator;
        this.transpositionTable = transpositionTable;
    }

    /**
     * Search the current position, increasing the depth each iteration, to find the best move within the given time limit.
     * @param timeControl the maximum duration to search
     * @return a {@link SearchResult} containing the best move, the eval, and other search info.
     */
    @Override
    public SearchResult search(TimeControl timeControl) {

        start = Instant.now();
        tc = timeControl;
        ss = new SearchStack();
        nodes = 0;
        currentDepth = 1;
        bestMoveRoot = null;
        bestMoveCurrentDepth = null;
        bestMoveStability = 0;
        bestScoreRoot = 0;
        bestScoreCurrentDepth = 0;
        cancelled = false;
        moveOrderer.ageHistoryScores(board.isWhiteToMove());

        int alpha = Score.MIN;
        int beta = Score.MAX;
        int retryMultiplier = 0;
        int aspMargin = config.getAspMargin();
        int aspFailMargin = config.getAspFailMargin();
        SearchResult result = null;

        while (!shouldStopSoft() && currentDepth < Search.MAX_DEPTH) {
            // Reset variables for the current depth iteration
            bestMoveCurrentDepth = null;
            bestScoreCurrentDepth = 0;

            // Perform alpha-beta search for the current depth
            int eval = search(currentDepth, 0, alpha, beta, true);

            // Update the best move and evaluation if a better move is found
            if (bestMoveCurrentDepth != null) {
                bestMoveStability = bestMoveRoot != null && bestMoveRoot.equals(bestMoveCurrentDepth) ? bestMoveStability + 1 : 0;
                bestMoveRoot = bestMoveCurrentDepth;
                bestScoreRoot = bestScoreCurrentDepth;
                result = buildResult();
                threadManager.handleSearchResult(result);
            }

            // Update the eval stability if the eval is stable
            evalStability = eval >= previousEval - 10 && eval <= previousEval + 10 ? evalStability + 1 : 0;

            // Check if search is cancelled or a checkmate is found
            if (shouldStop() || foundMate(currentDepth)) {
                break;
            }

            // Adjust the aspiration window in case the score fell outside the current window
            if (eval <= alpha) {
                // If score <= alpha, re-search with an expanded aspiration window
                retryMultiplier++;
                alpha -= aspFailMargin * retryMultiplier;
                continue;
            }
            if (eval >= beta) {
                // If score >= beta, re-search with an expanded aspiration window
                retryMultiplier++;
                beta += aspFailMargin * retryMultiplier;
                continue;
            }

            alpha = eval - aspMargin;
            beta = eval + aspMargin;

            // Increment depth and retry multiplier for next iteration
            retryMultiplier = 0;
            previousEval = eval;
            currentDepth++;
        }

        // If no move is found within the time limit, choose the first available move
        if (result == null) {
            System.out.println("Time expired before a move was found!");
            List<Move> legalMoves = moveGenerator.generateMoves(board);
            if (!legalMoves.isEmpty()) bestMoveRoot = legalMoves.get(0);
            result = buildResult();
        }

        // Clear move ordering cache and return the search result
        moveOrderer.clear();

        this.result = result;
        return result;

    }

    /**
     * Run a single iteration of the iterative deepening search for a specific depth.
     *
     * @param depth               The number of ply deeper left to go in the current search ('ply remaining').
     * @param ply                 The number of ply already examined in the current search ('ply from root').
     * @param alpha               The lower bound for child nodes at the current search depth.
     * @param beta                The upper bound for child nodes at the current search depth.
     * @param allowNull           Whether to allow null-move pruning in this search iteration.
     */
    public int search(int depth, int ply, int alpha, int beta, boolean allowNull) {

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

        MovePicker movePicker = new MovePicker(moveGenerator, moveOrderer, board, ss, ply);

        // Probe the transposition table in case this node has been searched before
        HashEntry transposition = transpositionTable.get(getKey(), ply);
        if (isUsefulTransposition(transposition, depth, alpha, beta)) {
            if (rootNode && transposition.getMove() != null) {
                bestMoveCurrentDepth = transposition.getMove();
                bestScoreCurrentDepth = transposition.getScore();
            }
            if (!pvNode) {
                return transposition.getScore();
            }
        }
        Move previousBestMove = rootNode ? bestMoveRoot : null;
        if (hasBestMove(transposition)) {
            previousBestMove = transposition.getMove();
        }
        movePicker.setTtMove(previousBestMove);

        boolean isInCheck = moveGenerator.isCheck(board, board.isWhiteToMove());

        // Internal Iterative Deepening - https://www.chessprogramming.org/Internal_Iterative_Deepening
        // If the position has not been searched yet, the search will be potentially expensive. So let's search with a
        // reduced depth expecting to record a move that we can use later for a full-depth search.
        if (!rootNode
                && !isInCheck
                && !hasBestMove(transposition)
                && ply > 0
                && depth >= config.getIirDepth()) {
            --depth;
        }

        // Re-use cached static eval if available. Don't compute static eval while in check.
        int staticEval = Integer.MIN_VALUE;
        if (!isInCheck) {
            staticEval = transposition != null ? transposition.getStaticEval() : evaluator.evaluate();
        }

        ss.setStaticEval(ply, staticEval);
        boolean improving = isImproving(ply, staticEval);

        if (!pvNode && !isInCheck) {
            // Reverse Futility Pruning - https://www.chessprogramming.org/Reverse_Futility_Pruning
            // If the static evaluation + some significant margin is still above beta, then let's assume this position
            // is a cut-node and will fail-high, and not search any further.
            boolean isMateHunting = Score.isMateScore(alpha);
            if (depth <= config.getRfpDepth()
                && staticEval - config.getRfpMargin()[depth] > beta
                && !isMateHunting) {
                return beta;
            }

            // Null Move Pruning - https://www.chessprogramming.org/Null_Move_Pruning
            // If the static evaluation + some significant margin is still above beta after giving the opponent two moves
            // in a row (making a 'null' move), then let's assume this position is a cut-node and will fail-high, and
            // not search any further.
            if (allowNull
                && depth >= config.getNmpDepth()
                && staticEval >= beta - (config.getNmpMargin() * (improving ? 1 : 0))
                && board.hasPiecesRemaining(board.isWhiteToMove())) {
                board.makeNullMove();
                int eval = -search(depth - 1 - (2 + depth / 3), ply + 1, -beta, -beta + 1, false);
                board.unmakeNullMove();
                if (eval >= beta) {
                    transpositionTable.put(getKey(), HashFlag.LOWER, depth, ply, previousBestMove, staticEval, beta);
                    return beta;
                }
            }
        }

        Move bestMove = null;
        HashFlag flag = HashFlag.UPPER;
        int movesSearched = 0;
        List<Move> quietsSearched = null;

        while (true) {

            Move move = movePicker.pickNextMove();
            if (move == null) break;
            if (bestMove == null) bestMove = move;
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
                && !isInCheck
                && !isCapture
                && !isPromotion) {
                movePicker.setSkipQuiets(true);
                continue;
            }

            evaluator.makeMove(board, move);
            if (!board.makeMove(move)) continue;
            ss.setMove(ply, move, piece);
            nodes++;

            boolean isCheck = moveGenerator.isCheck(board, board.isWhiteToMove());
            boolean isQuiet = !isCheck && !isCapture && !isPromotion;

            // Late Move Pruning - https://www.chessprogramming.org/Futility_Pruning#Move_Count_Based_Pruning
            // If the move is ordered very late in the list, and isn't a 'noisy' move like a check, capture or
            // promotion, let's assume it's less likely to be good, and fully skip searching that move.
            int lmpCutoff = (depth * config.getLmpMultiplier()) / (1 + (improving ? 0 : 1));
            if (!pvNode
                && !isInCheck
                && isQuiet
                && depth <= config.getLmpDepth()
                && movesSearched >= lmpCutoff) {
                evaluator.unmakeMove();
                board.unmakeMove();
                movePicker.setSkipQuiets(true);
                continue;
            }

            // Search Extensions - https://www.chessprogramming.org/Extensions
            // In certain interesting cases (e.g. promotions, or checks that do not immediately lose material), let's
            // extend the search depth by one ply.
            int extension = 0;
            if (isPromotion || (isCheck && SEEAfterMove.seeAfterMove(board, move) >= 0)) {
                extension = 1;
            }

            // Extend search 1 ply when entering a pawn endgame, to avoid accidentally trading into lost/drawn endgames
            if (isCapture && capturedPiece != Piece.PAWN && board.isPawnEndgame()) {
                extension = 1;
            }

            int eval;
            if (isDraw()) {
                eval = Score.DRAW;
            }
            else if (pvNode && movesSearched == 1) {
                // Principal Variation Search - https://www.chessprogramming.org/Principal_Variation_Search
                // The first move must be searched with the full alpha-beta window. If our move ordering is any good
                // then we expect this to be the best move, and so we need to retrieve the exact score.
                eval = -search(depth - 1 + extension, ply + 1, -beta, -alpha, true);
            }
            else {
                // Late Move Reductions - https://www.chessprogramming.org/Late_Move_Reductions
                // If the move is ordered late in the list, and isn't a 'noisy' move like a check, capture or promotion,
                // let's save time by assuming it's less likely to be good, and reduce the search depth.
                int reduction = 0;
                if (depth >= config.getLmrDepth()
                    && movesSearched >= (pvNode ? config.getLmrMinSearchedMoves() : config.getLmrMinSearchedMoves() - 1)
                    && isQuiet) {
                    reduction = config.getLmrReductions()[depth][movesSearched];
                    if (pvNode) {
                        reduction--;
                    }
                    if (transposition != null && transposition.getMove() != null && isCapture) {
                        reduction++;
                    }
                }
                // For all other moves apart from the principal variation, search with a null window (-alpha - 1, -alpha),
                // to try and prove the move will fail low while saving the time spent on a full search.
                eval = -search(depth - 1 + extension - reduction, ply + 1, -alpha - 1, -alpha, true);

                if (eval > alpha && (eval < beta || reduction > 0)) {
                    // If we reduced the depth and/or used a null window, and the score beat alpha, we need to do a
                    // re-search with the full window and depth. This is costly, but hopefully doesn't happen too often.
                    eval = -search(depth - 1 + extension, ply + 1, -beta, -alpha, true);
                }
            }

            evaluator.unmakeMove();
            board.unmakeMove();

            if (isQuiet && quietsSearched == null) {
                quietsSearched = new ArrayList<>();
            }

            if (shouldStop()) {
                return alpha;
            }

            if (eval >= beta) {

                // This is a beta cut-off - the opponent won't let us get here as they already have better alternatives
                transpositionTable.put(getKey(), HashFlag.LOWER, depth, ply, move, staticEval, beta);
                if (isQuiet) {
                    // Quiet moves which cause a beta cut-off are stored as 'killer' and 'history' moves for future move ordering
                    moveOrderer.addKillerMove(ply, move);
                    moveOrderer.addHistoryScore(move, ss, depth, ply, board.isWhiteToMove());
                    for (Move quiet : quietsSearched) {
                        moveOrderer.subHistoryScore(quiet, ss, depth, ply, board.isWhiteToMove());
                    }
                }

                return beta;
            }

            if (isQuiet) quietsSearched.add(move);

            if (eval > alpha) {
                // We have found a new best move
                bestMove = move;
                alpha = eval;
                flag = HashFlag.EXACT;
                if (rootNode) {
                    bestMoveCurrentDepth = move;
                    bestScoreCurrentDepth = eval;
                }
            }
        }

        if (movesSearched == 0) {
            // If there are no legal moves, and it's check, then it's checkmate. Otherwise, it's stalemate.
            return isInCheck ? -Score.MATE + ply : Score.DRAW;
        }
        if (rootNode && movesSearched == 1) {
            // If there is only one legal move at the root node, play that move immediately.
            int eval = isDraw() ? Score.DRAW : staticEval;
            bestMoveCurrentDepth = bestMove;
            bestScoreCurrentDepth = eval;
            cancelled = true;
            return eval;
        }

        transpositionTable.put(getKey(), flag, depth, ply, bestMove, staticEval, alpha);
        return alpha;

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

        QuiescentMovePicker movePicker = new QuiescentMovePicker(moveGenerator, moveOrderer, board);

        // Exit the quiescence search early if we already have an accurate score stored in the hash table.
        HashEntry transposition = transpositionTable.get(getKey(), ply);
        if (isUsefulTransposition(transposition, 1, alpha, beta)) {
            return transposition.getScore();
        }
        if (hasBestMove(transposition)) {
            movePicker.setTtMove(transposition.getMove());
        }

        boolean isInCheck = moveGenerator.isCheck(board, board.isWhiteToMove());

        // Re-use cached static eval if available. Don't compute static eval while in check.
        int eval = Integer.MIN_VALUE;
        if (!isInCheck) {
            eval = transposition != null ? transposition.getStaticEval() : evaluator.evaluate();
        }
        int standPat = eval;

        if (isInCheck) {
            // If we are in check, we need to generate 'all' legal moves that evade check, not just captures. Otherwise,
            // we risk missing simple mate threats.
            movePicker.setFilter(MoveFilter.ALL);
        } else {
            // If we are not in check, then we have the option to 'stand pat', i.e. decline to continue the capture chain,
            // if the static evaluation of the position is good enough.
            if (eval >= beta) {
                return beta;
            }
            if (eval > alpha) {
                alpha = eval;
            }
            MoveFilter filter = depth == 1 ? MoveFilter.NOISY : MoveFilter.CAPTURES_ONLY;
            movePicker.setFilter(filter);
        }

        int movesSearched = 0;

        while (true) {

            Move move = movePicker.pickNextMove();
            if (move == null) break;
            movesSearched++;

            if (!isInCheck) {
                // Delta Pruning - https://www.chessprogramming.org/Delta_Pruning
                // If the captured piece + a margin still has no potential of raising alpha, let's assume this position
                // is bad for us no matter what we do, and not bother searching any further
                Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(move.getTo());
                boolean isFutile = capturedPiece != null && (standPat + capturedPiece.getValue() + config.getDpMargin() < alpha) && !move.isPromotion();
                if (isFutile) {
                    continue;
                }
                // Static Exchange Evaluation - https://www.chessprogramming.org/Static_Exchange_Evaluation
                // Evaluate the possible captures + recaptures on the target square, in order to filter out losing capture
                // chains, such as capturing with the queen a pawn defended by another pawn.
                if ((depth <= 3 && !SEE.see(board, move, 0)) ||
                        (depth > 3 && !SEE.see(board, move, 1))) {
                    continue;
                }
            }

            evaluator.makeMove(board, move);
            if (!board.makeMove(move)) continue;
            nodes++;
            eval = isDraw() ? Score.DRAW : -quiescenceSearch(-beta, -alpha, depth + 1, ply + 1);
            evaluator.unmakeMove();
            board.unmakeMove();

            if (eval >= beta) {
                return beta;
            }
            if (eval > alpha) {
                alpha = eval;
            }
        }

        if (movesSearched == 0 && isInCheck) {
            return -Score.MATE + ply;
        }

        return alpha;

    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        this.evaluator.setPosition(board);
    }

    @Override
    public void setHashSize(int hashSizeMb) {
        this.transpositionTable = new TranspositionTable(hashSizeMb);
    }

    @Override
    public void setThreadCount(int threadCount) {
        // do nothing as this implementation is single-threaded
    }

    /**
     * Check if the hit from the transposition table is 'useful' in the current search. A TT-hit is useful either if it
     * 1) contains an exact evaluation, so we don't need to search any further, 2) contains a fail-high greater than our
     * current beta value, or 3) contains a fail-low lesser than our current alpha value.
     */
    private boolean isUsefulTransposition(HashEntry entry, int depth, int alpha, int beta) {
        return entry != null &&
                entry.getDepth() >= depth &&
                ((entry.getFlag().equals(HashFlag.EXACT)) ||
                 (entry.getFlag().equals(HashFlag.UPPER) && entry.getScore() <= alpha) ||
                 (entry.getFlag().equals(HashFlag.LOWER) && entry.getScore() >= beta));
    }

    private boolean hasBestMove(HashEntry transposition) {
        return transposition != null && transposition.getMove() != null;
    }

    private boolean foundMate(int currentDepth) {
        return Math.abs(bestScoreRoot) >= Score.MATE - currentDepth;
    }

    private SearchResult buildResult() {
        long millis = start != null ? Duration.between(start, Instant.now()).toMillis() : 0;
        long nps = nodes > 0 && millis > 0 ? ((nodes / millis) * 1000) : 0;
        return new SearchResult(bestScoreCurrentDepth, bestMoveCurrentDepth, currentDepth, millis, nodes, nps);
    }

    private boolean shouldStop() {
        // Exit if global search is cancelled
        if (config.isSearchCancelled()) return true;
        // Exit if local search is cancelled
        if (cancelled) return true;
        return !config.isPondering() && tc != null && tc.isHardLimitReached(start, currentDepth, nodes);
    }

    private boolean shouldStopSoft() {
        return !config.isPondering() && tc != null && tc.isSoftLimitReached(start, currentDepth, bestMoveStability, evalStability);
    }

    private boolean isDraw() {
        return Score.isEffectiveDraw(board);
    }

    private long getKey() {
        return board.getGameState().getZobrist();
    }

    public SearchResult getResult() {
        return result;
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

    @Override
    public TranspositionTable getTranspositionTable() {
        return transpositionTable;
    }

    @Override
    public void clearHistory() {
        transpositionTable.clear();
        evaluator.clearHistory();
    }

    @Override
    public void logStatistics() {
        //log.info(statistics.generateReport());
    }

}
