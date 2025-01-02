package com.kelseyde.calvin.movegen.sperft;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.search.Searcher;
import com.kelseyde.calvin.utils.TestUtils;
import com.kelseyde.calvin.utils.notation.FEN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class SPerftTest {

    private static final int MIN_EVAL = Integer.MIN_VALUE + 1;
    private static final int MAX_EVAL = Integer.MAX_VALUE - 1;
    private static final Searcher SEARCHER = TestUtils.SEARCHER;

    protected abstract String getFen();
    protected abstract String getSubFolder();

    protected void sPerft(int depth) {
        Board board = FEN.toBoard(getFen());
        Instant start = Instant.now();
        SEARCHER.clearHistory();
        SEARCHER.setPosition(board);
        SEARCHER.search(depth, 0, MIN_EVAL, MAX_EVAL, false);
        Instant end = Instant.now();
        Duration performance = Duration.between(start, end);
        writeResults(depth, performance);
    }

    private void writeResults(int depth, Duration performance) {
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String line = String.format("%s,%s\n", timestamp, performance);
        String fileName = String.format("src/test/resources/sperft/%s/sperft_depth_%s.csv", getSubFolder(), depth);
        Path path = Paths.get(fileName);
        try {
            Files.writeString(path, line, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.printf("Error writing to %s! %s", fileName, e);
        }
    }

}
