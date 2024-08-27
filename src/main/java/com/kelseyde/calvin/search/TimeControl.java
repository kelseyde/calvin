package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;

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
public record TimeControl(Duration softLimit, Duration hardLimit) {

    static final double SOFT_TIME_FACTOR = 0.6666;
    static final double HARD_TIME_FACTOR = 2.0;
    static final double[] BEST_MOVE_STABILITY_FACTOR = new double[] { 2.50, 1.20, 0.90, 0.80, 0.75 };

    public static TimeControl init(Board board, int timeWhiteMs, int timeBlackMs, int incWhiteMs, int incBlackMs) {

        boolean white = board.isWhiteToMove();
        double time = white ? timeWhiteMs : timeBlackMs;
        double inc = white ? incWhiteMs : incBlackMs;

        double base = time / 20 + inc * 0.75;
        Duration soft = Duration.ofMillis((int) (base * SOFT_TIME_FACTOR));
        Duration hard = Duration.ofMillis((int) (base * HARD_TIME_FACTOR));

        return new TimeControl(soft, hard);

    }

    public boolean isHardLimitReached(Instant start) {
        Duration expired = Duration.between(start, Instant.now());
        return expired.compareTo(hardLimit) > 0;
    }

    public boolean isSoftLimitReached(Instant start, int bestMoveStability) {
        Duration expired = Duration.between(start, Instant.now());
        Duration adjustedSoftLimit = adjustSoftLimit(softLimit, bestMoveStability);
        return expired.compareTo(adjustedSoftLimit) > 0;
    }

    private Duration adjustSoftLimit(Duration softLimit, int bestMoveStability) {

        // Scale the soft limit based on the stability of the best move. If the best move has remained stable for several
        // iterations, we can safely assume that we don't need to spend as much time searching further.
        bestMoveStability = Math.min(bestMoveStability, BEST_MOVE_STABILITY_FACTOR.length - 1);
        double bmStabilityFactor = BEST_MOVE_STABILITY_FACTOR[bestMoveStability];

        return Duration.ofMillis((long) (softLimit.toMillis() * bmStabilityFactor));
    }

}
