package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.search.ParallelSearcher;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.search.TimeControl;
import com.kelseyde.calvin.tables.tt.HashEntry;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import com.kelseyde.calvin.uci.UCICommand.PositionCommand;
import com.kelseyde.calvin.utils.notation.FEN;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The engine is responsible for actually playing a game of chess. It manages the game state, updates the board, and 'thinks'
 * (executes a search) to find the best move. It can also select how long to think for in the case of a time-controlled
 * game, or it can 'ponder' (think indefinitely when it is the opponent's turn).
 */
public class Engine {

    final EngineConfig config;
    final MoveGenerator movegen;
    final TranspositionTable tt;
    final Search searcher;

    CompletableFuture<SearchResult> think;
    Board board;

    public Engine() {
        this.config = new EngineConfig();
        this.board = Board.from(FEN.STARTPOS);
        this.movegen = new MoveGenerator();
        this.tt = new TranspositionTable(config.ttSizeDefault, config.ttBucketSize);
        this.searcher = new ParallelSearcher(config, tt);
        this.searcher.setPosition(board);
    }

    public void newGame() {
        searcher.clearHistory();
    }

    public void setPosition(PositionCommand command) {
        board = FEN.toBoard(command.fen());
        for (Move move : command.moves()) {
            Move legalMove = move(move);
            board.makeMove(legalMove);
        }
        searcher.setPosition(board);
    }

    public void setPosition(Board board) {
        this.board = board;
        searcher.setPosition(board);
    }

    public void setHashSize(int hashSizeMb) {
        this.searcher.setHashSize(hashSizeMb);
    }

    public void setThreadCount(int threadCount) {
        this.searcher.setThreadCount(threadCount);
    }

    public void setPonderEnabled(boolean ponderEnabled) {
        this.config.ponderEnabled = ponderEnabled;
    }

    public void setSearchCancelled(boolean cancelled) {
        this.config.searchCancelled = cancelled;
    }

    public void setPondering(boolean pondering) {
        this.config.pondering = pondering;
    }

    public void findBestMove(TimeControl tc, Consumer<SearchResult> onThinkComplete) {
        stopThinking();
        think = CompletableFuture.supplyAsync(() -> think(tc));
        think.thenAccept(onThinkComplete);
    }

    public SearchResult think(int timeout) {
        TimeControl tc = new TimeControl(Duration.ofMillis(timeout), Duration.ofMillis(timeout), -1, -1, -1);
        return searcher.search(tc);
    }

    public SearchResult think(TimeControl tc) {
        return searcher.search(tc);
    }

    public boolean isThinking() {
        return think != null && !think.isDone();
    }

    public void stopThinking() {
        if (isThinking()) {
            think.cancel(true);
        }
    }

    public void gameOver() {
        stopThinking();
        board = null;
    }

    public Move extractPonderMove(Move bestMove) {
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        board.makeMove(bestMove);
        long zobristKey = board.getState().getKey();
        HashEntry entry = transpositionTable.get(zobristKey, 0);
        board.unmakeMove();
        return entry != null ? entry.getMove() : null;
    }

    public List<Move> extractPrincipalVariation() {
        List<Move> principalVariation = new ArrayList<>();
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        int moveCount = 0;
        while (moveCount <= 12) {
            long zobristKey = board.getState().getKey();
            HashEntry entry = transpositionTable.get(zobristKey, 0);
            if (entry == null || entry.getMove() == null) {
                break;
            }
            principalVariation.add(entry.getMove());
            board.makeMove(entry.getMove());
            moveCount++;
        }
        IntStream.range(0, moveCount).forEach(i -> board.unmakeMove());
        return principalVariation;
    }

    public EngineConfig getConfig() {
        return config;
    }

    public Search getSearcher() {
        return searcher;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * For moves parsed from UCI, read from the opening book, or probed from the endgame tablebase, generate the
     * corresponding 'legal' move which includes any special move flag (promotion, en passant, castling etc.)
     */
    private Move move(Move move) {
        return movegen.generateMoves(board).stream()
                .filter(move::matches)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Illegal move " + Move.toUCI(move)));
    }

}
