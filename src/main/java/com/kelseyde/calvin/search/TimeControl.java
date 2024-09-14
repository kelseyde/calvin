package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.uci.UCICommand.GoCommand;

import java.time.Duration;
import java.time.Instant;

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

    static final double NODE_TIME_BASE = 1.5;
    static final double NODE_TIME_SCALE = 1.75;
    static final double NODE_TIME_MIN = 0.15;

    static final double SOFT_TIME_SCALE_MIN = 0.125;

    static final double[] BEST_MOVE_STABILITY_FACTOR = new double[] { 2.50, 1.20, 0.90, 0.80, 0.75 };
    static final double[] SCORE_STABILITY_FACTOR = new double[] { 1.25, 1.15, 1.00, 0.94, 0.88 };

    public static TimeControl init(Board board, GoCommand command) {

        double time;
        double inc;
        if (command.isMovetime()) {
            time = command.movetime();
            inc = 0;
        } else if (command.isTime()) {
            boolean white = board.isWhiteToMove();
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
            Instant start, int depth, int nodes, int bestMoveNodes, int bestMoveStability, int evalStability) {
        if (maxDepth > 0 && depth >= maxDepth) return true;
        if (maxNodes > 0 && nodes >= maxNodes) return true;
        Duration expired = Duration.between(start, Instant.now());
        Duration adjustedSoftLimit = adjustSoftLimit(softLimit, nodes, bestMoveNodes, bestMoveStability, evalStability);
        return expired.compareTo(adjustedSoftLimit) > 0;
    }

    private Duration adjustSoftLimit(
            Duration softLimit, int nodes, int bestMoveNodes, int bestMoveStability, int scoreStability) {

        double scale = 1.0;

        double bestMoveNodeFraction = (double) bestMoveNodes / nodes;

        // Scale the soft limit based on the fraction of total nodes spent searching the best move. If a greater portion
        // of the search has been spent on the best move, we can assume that the best move is more likely to be correct,
        // and therefore we can spend less time searching further.
        scale *= Math.max((NODE_TIME_BASE - bestMoveNodeFraction) * NODE_TIME_SCALE, NODE_TIME_MIN);

        // Scale the soft limit based on the stability of the best move. If the best move has remained stable for several
        // iterations, we can safely assume that we don't need to spend as much time searching further.
        bestMoveStability = Math.min(bestMoveStability, BEST_MOVE_STABILITY_FACTOR.length - 1);
        scale *= BEST_MOVE_STABILITY_FACTOR[bestMoveStability];

        // Scale the soft limit based on the stability of the search score. If the evaluation has remained stable for
        // several iterations, we can safely assume that we don't need to spend as much time searching further.
        scoreStability = Math.min(scoreStability, SCORE_STABILITY_FACTOR.length - 1);
        scale *= SCORE_STABILITY_FACTOR[scoreStability];

        scale = Math.max(scale, SOFT_TIME_SCALE_MIN);

        return Duration.ofMillis((long) (softLimit.toMillis() * scale));
    }

}
