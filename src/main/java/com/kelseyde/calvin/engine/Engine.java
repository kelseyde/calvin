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
import com.kelseyde.calvin.uci.UCI;
import com.kelseyde.calvin.uci.UCICommand.GoCommand;
import com.kelseyde.calvin.uci.UCICommand.PositionCommand;
import com.kelseyde.calvin.utils.notation.FEN;
import com.kelseyde.calvin.utils.perft.PerftService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * The engine is responsible for actually playing a game of chess. It manages the game state, updates the board, and
 * 'thinks' (executes a search) to find the best move. It can also select how long to think for in the case of a
 * time-controlled game, or it can 'ponder' (think indefinitely when it is the opponent's turn).
 */
public class Engine {

    // Singleton pattern: only one instance of the engine can exist.
    private static Engine INSTANCE;

    public static Engine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Engine();
        }
        return INSTANCE;
    }

    final EngineConfig config;
    final MoveGenerator movegen;
    final PerftService perft;
    final Search searcher;

    CompletableFuture<SearchResult> think;
    Board board;

    private Engine() {
        this.config = new EngineConfig();
        this.board = Board.from(FEN.STARTPOS);
        this.movegen = new MoveGenerator();
        this.perft = new PerftService();
        this.searcher = new ParallelSearcher(config, new TranspositionTable(config.defaultHashSizeMb));
        this.searcher.setPosition(board);
    }

    public void newGame() {
        searcher.clearHistory();
        board = Board.from(FEN.STARTPOS);
        searcher.setPosition(board);
    }

    public void setPosition(PositionCommand command) {
        board = FEN.toBoard(command.fen());
        for (Move move : command.moves()) {
            Move legalMove = move(move);
            board.makeMove(legalMove);
        }
        searcher.setPosition(board);
    }

    public void go(GoCommand command) {

        if (command.isPerft()) {
            int depth = command.perft();
            perft.perft(board, depth);
        } else {
            this.config.pondering = command.ponder();
            setSearchCancelled(false);
            TimeControl tc = TimeControl.init(config, board, command);
            stopThinking();
            think = CompletableFuture.supplyAsync(() -> think(tc));
            think.thenAccept(UCI::writeMove);
        }

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

    public Move extractPonderMove(Move bestMove) {
        TranspositionTable tt = searcher.getTranspositionTable();
        board.makeMove(bestMove);
        long key = board.key();
        HashEntry entry = tt.get(key, 0);
        board.unmakeMove();
        return entry != null ? entry.move() : null;
    }

    public List<Move> extractPrincipalVariation() {
        List<Move> pv = new ArrayList<>();
        TranspositionTable tt = searcher.getTranspositionTable();
        int moves = 0;
        while (moves < 24) {
            long key = board.key();
            HashEntry entry = tt.get(key, 0);
            if (entry == null || entry.move() == null) {
                break;
            }
            pv.add(entry.move());
            board.makeMove(entry.move());
            moves++;
        }
        IntStream.range(0, moves).forEach(i -> board.unmakeMove());
        return pv;
    }

    public int hashfull() {
        return searcher.getTranspositionTable().fill();
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
        return movegen.generateMoves(board).toList().stream()
                .filter(move::matches)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Illegal move " + Move.toUCI(move)));
    }

}
