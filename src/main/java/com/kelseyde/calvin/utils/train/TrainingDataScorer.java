package com.kelseyde.calvin.utils.train;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.search.Searcher;
import com.kelseyde.calvin.search.ThreadManager;
import com.kelseyde.calvin.search.TimeControl;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TrainingDataScorer {

    private static final int THREAD_COUNT = 20;
    private static final int BATCH_SIZE = THREAD_COUNT * 1000;
    private static final int TT_SIZE = 64;
    private static final Duration MAX_SEARCH_TIME = Duration.ofSeconds(30);

    private List<Searcher> searchers;

    public void score(ScoreDataCommand command) {

        System.out.printf("Scoring training data from %s to %s with soft limit %d and resume offset %d\n",
                command.inputFile(), command.outputFile(), command.softNodeLimit(), command.resumeOffset());
        Path inputPath = Paths.get(command.inputFile());
        Path outputPath = Paths.get(command.outputFile());
        UCI.outputEnabled = false;
        searchers = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> initSearcher())
                .toList();
        Instant start = Instant.now();

        try (Stream<String> lines = Files.lines(inputPath)) {
            if (!Files.exists(outputPath)) Files.createFile(outputPath);
            int count = 0;
            AtomicInteger scored = new AtomicInteger(0);
            List<String> batch = new ArrayList<>(BATCH_SIZE);
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                if (count++ < command.resumeOffset()) {
                    iterator.next();
                    continue;
                }
                if (scored.get() > 0 && batch.isEmpty()) {
                    Duration duration = Duration.between(start, Instant.now());
                    System.out.println("info string progress: scored " + scored + " positions, total time " + duration);
                }
                String line = iterator.next();
                batch.add(line);
                if (batch.size() == BATCH_SIZE) {
                    List<String> scoredBatch = processBatch(batch, command.softNodeLimit());
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

    private List<String> processBatch(List<String> positions, int softLimit) {
        List<List<String>> partitions = partitionBatch(positions);
        List<Future<List<String>>> futures = new ArrayList<>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            Searcher searcher = searchers.get(i);
            List<String> partition = partitions.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                List<String> scoredPartition = new ArrayList<>(partition.size());
                for (String line : partition) {
                    String scoredLine = scoreData(searcher, line, softLimit);
                    if (!scoredLine.isEmpty()) {
                        scoredPartition.add(scoredLine);
                    }
                }
                return scoredPartition;
            }));
        }
        List<String> scoredBatch = new ArrayList<>(positions.size());
        for (Future<List<String>> future : futures) {
            try {
                scoredBatch.addAll(future.get());
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


    private String scoreData(Searcher searcher, String line, int softNodeLimit) {
        String[] parts = line.split("\\|");
        String fen = parts[0].trim();
        String result = parts[2].trim();
        Board board = FEN.toBoard(fen);
        searcher.setPosition(board);
        TimeControl tc = new TimeControl(MAX_SEARCH_TIME, MAX_SEARCH_TIME, softNodeLimit, -1);
        SearchResult searchResult;
        try {
             searchResult = searcher.search(tc);
        } catch (Exception e) {
            System.out.println("info error scoring fen " + fen + " " + e);
            return "";
        }
        int score = searchResult.eval();
        if (!board.isWhiteToMove()) score = -score;
        return String.format("%s | %s | %s", fen, score, result);
    }

    private Searcher initSearcher() {
        EngineConfig config = EngineInitializer.loadDefaultConfig();
        MoveGeneration moveGenerator = new MoveGenerator();
        MoveOrdering moveOrderer = new MoveOrderer();
        TranspositionTable transpositionTable = new TranspositionTable(TT_SIZE);
        ThreadManager threadManager = new ThreadManager();
        Evaluation evaluator = new NNUE();
        return new Searcher(config, threadManager, moveGenerator, moveOrderer, evaluator, transpositionTable);
    }

}
