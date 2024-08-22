package com.kelseyde.calvin.engine;

import java.time.Duration;

public class Time {

    public static record TimeLimit(Duration softLimit, Duration hardLimit) {}

    // Allow for some overhead in UCI communication
    public static final int OVERHEAD_MS = 20;

    // How often we should we check for timeout TODO nodes?
    public static final int CHECKUP_WINDOW = 4096;

    public static TimeLimit chooseThinkTime(int timeMs, int incMs) {
        timeMs -= OVERHEAD_MS;
        double optimalThinkTime = Math.min(timeMs * 0.5, timeMs * 0.03333 + incMs);
        double minThinkTime = Math.min(50, (int) (timeMs * 0.25));
        double thinkTime = Math.max(optimalThinkTime, minThinkTime);
        return (int) thinkTime;
    }

}
