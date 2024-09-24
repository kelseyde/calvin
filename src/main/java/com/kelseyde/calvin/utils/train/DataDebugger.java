package com.kelseyde.calvin.utils.train;

import com.kelseyde.calvin.movegen.MoveGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Iterator;
import java.util.stream.Stream;

public class DataDebugger {

    private static final int THREAD_COUNT = 20;
    private static final int THREAD_TIMEOUT_SECONDS = 15;
    private static final int BATCH_SIZE = THREAD_COUNT * 1000;
    private static final int TT_SIZE = 64;
    private static final int TOTAL_POSITIONS_PER_FILE = 100000000;
    private static final Duration MAX_SEARCH_TIME = Duration.ofSeconds(30);
    private static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();

    private int count = 0;
    private int maxScore = Integer.MIN_VALUE;
    private int minScore = Integer.MAX_VALUE;
    private long totalScore = 0;

    //private final Set<Long> keys = new HashSet<>();

    private int wonScores = 0;

    public void score() {

        Path inputPath = Paths.get("/Users/kelseyde/git/dan/calvin/data/calvindata_6.txt");

        try (Stream<String> lines = Files.lines(inputPath)) {

            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {

                count++;
                if (count % 50000 == 0) {
                    System.out.printf("count: %d\n", count);
                }

                String line = iterator.next();
                String[] parts = line.split("\\|");
                int score = Integer.parseInt(parts[1].trim());
                totalScore += score;
                if (score > maxScore) {
                    maxScore = score;
                }
                if (score < minScore) {
                    minScore = score;
                }
                if (Math.abs(score) > 7500) {
                    wonScores++;
                }
            }

            //System.out.printf("duplicate keys: %d\n", count - keys.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }

        System.out.println("total positions: " + count);

    }

    public static void main(String[] args) {
        DataDebugger dataDebugger = new DataDebugger();
        dataDebugger.score();
    }

}
