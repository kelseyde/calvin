package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.transposition.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * The ParallelSearcher class is an implementation of the {@link Search} interface that uses a parallel search strategy
 * called 'Lazy SMP'. The idea is to have multiple threads searching the same position simultaneously, but sharing a
 * {@link TranspositionTable}, so each thread benefits from the work of the others. Each thread is simply a Searcher
 * that runs its own iterative deepening loop.
 * </p>
 * Lazy SMP (Symmetric MultiProcessing) is a simple yet effective technique for parallelizing search trees. It involves
 * running multiple instances of the search algorithm on the same position, with each instance running on a separate CPU
 * core. The instances share a transposition table, which allows them to benefit from each other's work. The "lazy" part
 * of the name comes from the fact that there is no explicit work division or synchronization between the threads - they
 * each do their own thing, but can use the results of the others' work when they come across the same positions.
 *
 * @see <a href="https://www.chessprogramming.org/Lazy_SMP">Chess Programming Wiki</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParallelSearcher implements Search {

    final EngineConfig config;
    final Supplier<MoveGeneration> moveGeneratorSupplier;
    final Supplier<MoveOrdering> moveOrdererSupplier;
    final Supplier<Evaluation> evaluatorSupplier;
    final ThreadManager threadManager;
    TranspositionTable transpositionTable;
    int threadCount;
    int hashSize;
    Board board;
    private List<Searcher> searchers;

    /**
     * Constructs a ParallelSearcher with the given {@link EngineConfig} config and {@link Supplier} suppliers.
     * The suppliers are used to create the necessary components for each search thread.
     *
     * @param config the engine configuration
     * @param moveGeneratorSupplier supplier for move generation
     * @param moveOrdererSupplier supplier for move ordering
     * @param evaluatorSupplier supplier for evaluation
     * @param transpositionTable the shared transposition table
     */
    public ParallelSearcher(EngineConfig config,
                            Supplier<MoveGeneration> moveGeneratorSupplier,
                            Supplier<MoveOrdering> moveOrdererSupplier,
                            Supplier<Evaluation> evaluatorSupplier,
                            TranspositionTable transpositionTable) {
        this.config = config;
        this.hashSize = config.getDefaultHashSizeMb();
        this.threadCount = config.getDefaultThreadCount();
        this.threadManager = new ThreadManager();
        this.transpositionTable = transpositionTable;
        this.moveGeneratorSupplier = moveGeneratorSupplier;
        this.moveOrdererSupplier = moveOrdererSupplier;
        this.evaluatorSupplier = evaluatorSupplier;
        this.searchers = initSearchers();
    }

    /**
     * Searches for the best move within the given duration. Does so by starting a new thread for each searcher and
     * waiting for all threads to finish. The best result is then selected from the results of the individual searchers.
     *
     * @param timeControl the maximum duration to search
     * @return the best search result found
     */
    @Override
    public SearchResult search(TimeControl timeControl) {
        try {
            setPosition(board);
            threadManager.reset();
            List<CompletableFuture<SearchResult>> threads = searchers.stream()
                    .map(searcher -> initThread(searcher, timeControl))
                    .toList();

            SearchResult result = selectResult(threads).get();
            transpositionTable.incrementGeneration();
            return result;
        } catch (Exception e) {
            System.out.println("info error " + e);
            return SearchResult.empty();
        }
    }

    /**
     * Sets the current board position for all searchers. This is done by copying the board to each searcher.
     * This is necessary because each searcher will modify the board during the search, so we need to ensure that
     * each searcher has its own copy of the board.
     *
     * @param board the board to set
     */
    @Override
    public void setPosition(Board board) {
        this.board = board;
        searchers.forEach(searcher -> searcher.setPosition(board.copy()));
    }

    @Override
    public void setNodeLimit(int nodeLimit) {
        searchers.forEach(searcher -> searcher.setNodeLimit(nodeLimit));
    }

    /**
     * Sets the size of the {@link TranspositionTable}. This is done by creating a new transposition table with the
     * given size and setting it for all searchers.
     *
     * @param hashSizeMb the size in megabytes
     */
    @Override
    public void setHashSize(int hashSizeMb) {
        this.hashSize = hashSizeMb;
        this.transpositionTable = new TranspositionTable(this.hashSize);
        this.searchers = initSearchers();
    }

    /**
     * Sets the number of threads to use for searching.
     *
     * @param threadCount the number of threads
     */
    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        this.searchers = initSearchers();
    }

    private CompletableFuture<SearchResult> initThread(Searcher searcher, TimeControl tc) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return searcher.search(tc);
            } catch (Exception e) {
                System.out.printf("info error %s, %s %s%n", e.getMessage(), e.getCause(), Arrays.toString(e.getStackTrace()));
                return SearchResult.empty();
            }
        });
    }

    /**
     * Combines the {@link SearchResult} results of the different threads and selects a final result to use.
     * Simply selects the result from the thread which searched to the greatest depth.
     *
     * @return the best search result
     */
    private CompletableFuture<SearchResult> selectResult(List<CompletableFuture<SearchResult>> threads) {
        CompletableFuture<SearchResult> collector = CompletableFuture.completedFuture(new SearchResult(0, null, 0, 0, 0, 0));
        for (CompletableFuture<SearchResult> thread : threads) {
            collector = collector.thenCombine(thread, (thread1, thread2) -> thread1.depth() > thread2.depth() ? thread1 : thread2);
        }
        return collector;
    }

    /**
     * Initializes the list of searchers.
     *
     * @return the list of initialized searchers
     */
    private List<Searcher> initSearchers() {
        return IntStream.range(0, threadCount).mapToObj(i -> initSearcher()).toList();
    }

    /**
     * Initializes a single searcher with the necessary components.
     *
     * @return the initialized searcher
     */
    private Searcher initSearcher() {
        MoveGeneration moveGenerator = this.moveGeneratorSupplier.get();
        MoveOrdering moveOrderer = this.moveOrdererSupplier.get();
        Evaluation evaluator = this.evaluatorSupplier.get();
        return new Searcher(config, threadManager, moveGenerator, moveOrderer, evaluator, transpositionTable);
    }

    /**
     * Returns the transposition table used by the searchers.
     *
     * @return the transposition table
     */
    @Override
    public TranspositionTable getTranspositionTable() {
        return transpositionTable;
    }

    /**
     * Clears the history in the transposition table and all searchers.
     */
    @Override
    public void clearHistory() {
        transpositionTable.clear();
        searchers.forEach(Searcher::clearHistory);
    }

    @Override
    public void logStatistics() {
        System.out.println("info threads " + searchers.stream().map(Searcher::toString).toList());
    }

}
