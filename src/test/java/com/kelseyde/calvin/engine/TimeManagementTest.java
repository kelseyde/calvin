package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.utils.notation.Notation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class TimeManagementTest {

    Engine engine;

    @BeforeEach
    public void beforeEach() {
        engine = EngineInitializer.loadEngine();
        engine.setBoard(new Board());
    }

    @Test
    public void testRapid() {
        simulateGame(Duration.ofMinutes(10), Duration.ofSeconds(2), 100);
    }

    @Test
    public void testBlitz() {
        simulateGame(Duration.ofMinutes(3), Duration.ofSeconds(2), 100);
    }

    @Test
    public void testBullet() {
        simulateGame(Duration.ofMinutes(1), Duration.ofSeconds(1), 100);
    }

    private void simulateGame(Duration time, Duration increment, int totalMoves) {

        Duration timeRemaining = time;
        Duration thinkTime;
        Duration overhead = Duration.ofMillis(50);
        for (int move = 0; move < totalMoves; move++) {
            addMove();
            thinkTime = Duration.ofMillis(engine.chooseThinkTime((int) timeRemaining.toMillis(), 0, (int) increment.toMillis(), 0));
            System.out.printf("Move %s, Time %s, Think %s%n", move, timeRemaining, thinkTime);
            timeRemaining = timeRemaining.minus(thinkTime).plus(increment).minus(overhead);
        }


    }

    private void addMove() {
        engine.getBoard().getMoveHistory().add(Notation.fromCombinedNotation("e2e4"));
    }


}