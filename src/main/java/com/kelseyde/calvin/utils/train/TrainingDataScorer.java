package com.kelseyde.calvin.utils.train;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.*;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import com.kelseyde.calvin.uci.UCI;
import com.kelseyde.calvin.uci.UCICommand.ScoreDataCommand;
import com.kelseyde.calvin.utils.FEN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TrainingDataScorer {

    // 2220 pos/s current avg

    private static final int THREAD_COUNT = 20;
    private static final int THREAD_TIMEOUT_SECONDS = 15;
    private static final int BATCH_SIZE = THREAD_COUNT * 1000;
    private static final int TT_SIZE = 64;
    private static final int TOTAL_POSITIONS_PER_FILE = 100000000;
    private static final Duration MAX_SEARCH_TIME = Duration.ofSeconds(30);
    private static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();

    private List<Searcher> searchers;

    public void score(ScoreDataCommand command) {

        System.out.printf("Scoring training data from %s to %s, soft limit %d, hard limit %d, resume offset %d\n",
                command.inputFile(), command.outputFile(), command.softNodes(), command.hardNodes(), command.resumeOffset());
        Path inputPath = Paths.get(command.inputFile());
        Path outputPath = Paths.get(command.outputFile());
        UCI.outputEnabled = false;
        searchers = IntStream.range(0, THREAD_COUNT)
                .mapToObj(this::initSearcher)
                .toList();
        Instant start = Instant.now();

        try (Stream<String> lines = Files.lines(inputPath)) {
            if (!Files.exists(outputPath)) Files.createFile(outputPath);
            int count = 0;
            AtomicInteger scored = new AtomicInteger(0);
            AtomicInteger excluded = new AtomicInteger(0);
            List<String> batch = new ArrayList<>(BATCH_SIZE);
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                if (count++ < command.resumeOffset()) {
                    iterator.next();
                    continue;
                }
                if (scored.get() > 0 && batch.isEmpty()) {
                    logProgress(start, command, scored, excluded);
                }
                String line = iterator.next();
                batch.add(line);
                if (batch.size() == BATCH_SIZE) {
                    List<String> scoredBatch = processBatch(batch, command);
                    excluded.addAndGet(batch.size() - scoredBatch.size());
                    batch.clear();
                    scored.addAndGet(scoredBatch.size());
                    searchers.forEach(Searcher::clearHistory);
                    try {
                        Files.write(outputPath, scoredBatch, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write output file", e);
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }

        UCI.outputEnabled = true;
    }

    private List<String> processBatch(List<String> positions, ScoreDataCommand command) {
        List<List<String>> partitions = partitionBatch(positions);
        List<Future<List<String>>> futures = new ArrayList<>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            Searcher searcher = searchers.get(i);
            List<String> partition = partitions.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<String> scoredPartition = new ArrayList<>(partition.size());
                    for (String line : partition) {
                        String scoredLine = scoreData(searcher, line, command);
                        if (!scoredLine.isEmpty()) {
                            scoredPartition.add(scoredLine);
                        }
                    }
                    return scoredPartition;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to score partition " + Arrays.toString(e.getStackTrace()) + e.getCause() + e.getMessage(), e);
                }
            }));
        }
        List<String> scoredBatch = new ArrayList<>(positions.size());
        for (Future<List<String>> future : futures) {
            try {
                scoredBatch.addAll(future.get(THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new RuntimeException("Failed to score batch " + Arrays.toString(e.getStackTrace()) + e.getCause() + e.getMessage(), e);
            }
        }
        return scoredBatch;
    }

    private List<List<String>> partitionBatch(List<String> batch) {
        List<List<String>> partitions = new ArrayList<>(THREAD_COUNT);
        int partitionSize = batch.size() / THREAD_COUNT;
        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * partitionSize;
            int end = (i + 1) * partitionSize;
            if (i == THREAD_COUNT - 1) {
                end = batch.size();
            }
            partitions.add(batch.subList(start, end));
        }
        return partitions;
    }


    private String scoreData(Searcher searcher, String line, ScoreDataCommand command) {
        String[] parts = line.split("\\|");
        String fen = parts[0].trim();
        String result = parts[2].trim();
        Board board = FEN.toBoard(fen);
        if (MOVE_GENERATOR.isCheck(board, board.isWhite())) {
            // Filter out positions where the side to move is in check
            return "";
        }
        searcher.setPosition(board);
        TimeControl tc = new TimeControl(MAX_SEARCH_TIME, MAX_SEARCH_TIME, command.softNodes(), command.hardNodes(), -1);
        SearchResult searchResult;
        try {
             searchResult = searcher.search(tc);
        } catch (Exception e) {
            System.out.println("info error scoring fen " + fen + " " + e);
            return "";
        }
        Move bestMove = searchResult.move();
        if (bestMove == null) {
            // Filter out positions where there is no best move
            return "";
        }
        boolean isCapture = board.pieceAt(bestMove.to()) != null;
        if (isCapture) {
            // Filter out positions where the best move is a capture
            return "";
        }
        int score = searchResult.eval();
        if (Score.isMateScore(score)) {
            // Filter out positions where there is forced mate
            return "";
        }
        if (!board.isWhite()) score = -score;
        return String.format("%s | %s | %s", fen, score, result);
    }

    private Searcher initSearcher(int i) {
        EngineConfig config = EngineInitializer.loadDefaultConfig();
        TranspositionTable transpositionTable = new TranspositionTable(TT_SIZE);
        return new Searcher(config, transpositionTable, new ThreadData(i == 0));
    }

    private void logProgress(Instant start, ScoreDataCommand command, AtomicInteger scored, AtomicInteger excluded) {
        Duration duration = Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.SECONDS);
        int total = scored.get() + excluded.get() + command.resumeOffset();
        int totalSinceResume = total - command.resumeOffset();
        int remaining = TOTAL_POSITIONS_PER_FILE - total;
        double rate = (double) totalSinceResume / duration.getSeconds();
        String rateFormatted = String.format("%.0f", rate);
        Duration estimate = Duration.ofSeconds((long) (remaining / rate)).truncatedTo(ChronoUnit.SECONDS);
        System.out.printf("processed %d, since resume %d, scored %d, excluded %d, time %s, pos/s %s, remaining pos %s remaining time %s\n",
                total, totalSinceResume, scored.get(), excluded.get(), duration, rateFormatted, remaining, estimate);
    }

}
