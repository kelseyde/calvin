package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.uci.UCICommand.GoCommand;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The amount of time the engine chooses to search is split into to two limits: hard and soft. The hard limit is checked
 * constantly during search, and the search is aborted as soon as it is reached. The soft limit is checked at the start
 * of each iterative deepening loop, and the engine does not bother starting a new search if it is reached.
 * </p>
 * The idea is that if the engine is unlikely to finish a new iteration before hitting the hard limit, then there's no
 * point starting the iteration, since the time spent doing so is mostly wasted. That time can therefore be saved for
 * subsequent moves.
 * @param softLimit
 * @param hardLimit
 */
public record TimeControl(Duration softLimit, Duration hardLimit, int maxNodes, int maxDepth) {

    static final double SOFT_TIME_FACTOR = 0.6666;
    static final double HARD_TIME_FACTOR = 2.0;
    static final double[] BEST_MOVE_STABILITY_FACTOR = new double[] { 2.50, 1.20, 0.90, 0.80, 0.75 };
    static final double[] EVAL_STABILITY_FACTOR = new double[] { 1.25, 1.15, 1.00, 0.94, 0.88 };
    static final int EVAL_STABILITY_MIN_DEPTH = 7;

    public static TimeControl init(Board board, GoCommand command) {

        boolean white = board.isWhiteToMove();

        double time;
        double inc;
        if (command.isMovetime()) {
            time = command.movetime();
            inc = 0;
        } else if (command.isTime()) {
            time = white ? command.wtime() : command.btime();
            inc = white ? command.winc() : command.binc();
        } else {
            time = Double.MAX_VALUE;
            inc = 0;
        }

        // If we were sent negative time, just assume we have one second.
        if (time <= 0) time = 1000;
        if (inc < 0) inc = 0;

        double base = time / 20 + inc * 0.75;
        Duration soft = Duration.ofMillis((int) (base * SOFT_TIME_FACTOR));
        Duration hard = Duration.ofMillis((int) (base * HARD_TIME_FACTOR));

        return new TimeControl(soft, hard, command.nodes(), command.depth());

    }

    public boolean isHardLimitReached(Instant start, int depth, int nodes) {
        if (nodes % 4096 != 0) return false;
        if (maxDepth > 0 && depth >= maxDepth) return true;
        Duration expired = Duration.between(start, Instant.now());
        return expired.compareTo(hardLimit) > 0;
    }

    public boolean isSoftLimitReached(
            Instant start, int depth, int nodes, int bestMoveStability, int evalStability, List<Move> legalMoves) {
        if (maxDepth > 0 && depth >= maxDepth) return true;
        if (maxNodes > 0 && nodes >= maxNodes) return true;
        Duration expired = Duration.between(start, Instant.now());
        Duration adjustedSoftLimit = adjustSoftLimit(softLimit, depth, bestMoveStability, evalStability, legalMoves);
        return expired.compareTo(adjustedSoftLimit) > 0;
    }

    private Duration adjustSoftLimit(
            Duration softLimit, int depth, int bestMoveStability, int evalStability, List<Move> legalMoves) {

        // Scale the soft limit based on the stability of the best move. If the best move has remained stable for several
        // iterations, we can safely assume that we don't need to spend as much time searching further.
        bestMoveStability = Math.min(bestMoveStability, BEST_MOVE_STABILITY_FACTOR.length - 1);
        double bmStabilityFactor = BEST_MOVE_STABILITY_FACTOR[bestMoveStability];

        // Scale the soft limit based on the stability of the evaluation. If the evaluation has remained stable for several
        // iterations, we can safely assume that we don't need to spend as much time searching further.
        evalStability = Math.min(evalStability, EVAL_STABILITY_FACTOR.length - 1);
        double evalStabilityFactor = EVAL_STABILITY_FACTOR[evalStability];

        // Scale the soft limit based on the number of legal moves. If there are fewer legal moves, it is likely we are in 
        // a more tactical position, requiring deeper analysis to walk the fine line to safety. Conversely, if there are 
        // many legal moves, the position may be calmer with several reasonable options.
        int moveCount = legalMoves.size();
        double moveCountFactor = scaleTimeByMoveCount(moveCount);

        double adjustedLimit = softLimit.toMillis() * bmStabilityFactor * moveCountFactor;
        if (depth >= EVAL_STABILITY_MIN_DEPTH) {
            adjustedLimit *= evalStabilityFactor;
        }

        return Duration.ofMillis((long) adjustedLimit);
    }

    private double scaleTimeByMoveCount(int moveCount) {
        // Define the limits for interpolation
        final int minMoves = 2;
        final int maxMoves = 40;

        // Define the scaling factors for the extremes
        final double minFactor = 1.25;
        final double maxFactor = 0.75;

        // Ensure moveCount is clamped between the min and max limits
        moveCount = clamp(moveCount, minMoves, maxMoves);

        // Calculate the slope for linear scaling between minMoves and maxMoves
        double slope = (maxFactor - minFactor) / (maxMoves - minMoves);

        // Compute the scale factor based on the number of moves
        return minFactor + slope * (moveCount - minMoves);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}
