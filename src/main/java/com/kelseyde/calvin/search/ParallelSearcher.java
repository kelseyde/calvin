package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.tables.tt.TranspositionTable;

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
public class ParallelSearcher implements Search {

    private final EngineConfig config;
    private final TranspositionTable tt;
    private final MoveGenerator movegen;

    private List<Searcher> searchers;
    private int threadCount;
    private int hashSize;
    private Board board;

    /**
     * Constructs a ParallelSearcher with the given {@link EngineConfig} config and {@link Supplier} suppliers.
     * The suppliers are used to create the necessary components for each search thread.
     *
     * @param config the engine configuration
     * @param tt the shared transposition table
     */
    public ParallelSearcher(EngineConfig config, MoveGenerator movegen, TranspositionTable tt) {
        this.tt = tt;
        this.config = config;
        this.movegen = movegen;
        this.hashSize = config.defaultHashSizeMb;
        this.threadCount = config.defaultThreads;
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
            List<CompletableFuture<SearchResult>> threads = searchers.stream()
                    .map(searcher -> initThread(searcher, timeControl))
                    .toList();

            SearchResult result = selectResult(threads).get();
            tt.incrementAge();
            return result;
        } catch (Exception e) {
            System.out.println("info error " + e);
            // In case of an error, return a random legal move to avoid crashing the engine
            Move move = movegen.generateMoves(board).stream().findAny().orElse(null);
            return SearchResult.of(move);
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

    /**
     * Sets the size of the {@link TranspositionTable}. This is done by creating a new transposition table with the
     * given size and setting it for all searchers.
     *
     * @param hashSizeMb the size in megabytes
     */
    @Override
    public void setHashSize(int hashSizeMb) {
        this.hashSize = hashSizeMb;
        this.tt.resize(this.hashSize);
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
                // In case of an error, return a random legal move to avoid crashing the engine
                Move move = movegen.generateMoves(board).stream().findAny().orElse(null);
                return SearchResult.of(move);
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
        CompletableFuture<SearchResult> collector = CompletableFuture.completedFuture(new SearchResult(0, null, 0, 0, 0, 0, new Move[0]));
        for (CompletableFuture<SearchResult> thread : threads) {
            collector = collector.thenCombine(thread, (thread1, thread2) -> {
                SearchResult best = thread1.depth() > thread2.depth() ? thread1 : thread2;
                int bestEval = best.eval();
                Move bestMove = best.move();
                return new SearchResult(bestEval, bestMove, best.depth(), best.time(), thread1.nodes() + thread2.nodes(), best.nps(), best.pv());
            });
        }
        return collector;
    }

    /**
     * Initializes the list of searchers.
     *
     * @return the list of initialized searchers
     */
    private List<Searcher> initSearchers() {
        return IntStream.range(0, threadCount)
                .mapToObj(i -> initSearcher(i == 0))
                .toList();
    }

    /**
     * Initializes a single searcher with the necessary components.
     *
     * @return the initialized searcher
     */
    private Searcher initSearcher(boolean mainThread) {
        ThreadData td = new ThreadData(mainThread);
        return new Searcher(config, tt, td);
    }

    /**
     * Returns the transposition table used by the searchers.
     *
     * @return the transposition table
     */
    @Override
    public TranspositionTable getTranspositionTable() {
        return tt;
    }

    /**
     * Clears the history in the transposition table and all searchers.
     */
    @Override
    public void clearHistory() {
        tt.clear();
        searchers.forEach(Searcher::clearHistory);
    }

}
