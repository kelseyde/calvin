package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.endgame.TablebaseException;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.search.TimeControl;
import com.kelseyde.calvin.transposition.HashEntry;
import com.kelseyde.calvin.transposition.TranspositionTable;
import com.kelseyde.calvin.uci.UCICommand.PositionCommand;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

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
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Engine {

    EngineConfig config;
    OpeningBook book;
    Tablebase tablebase;
    MoveGeneration moveGenerator;
    Search searcher;
    CompletableFuture<SearchResult> think;
    Board board;

    public Engine(EngineConfig config, OpeningBook book, Tablebase tablebase, MoveGeneration moveGenerator, Search searcher) {
        this.config = config;
        this.book = book;
        this.tablebase = tablebase;
        this.moveGenerator = moveGenerator;
        this.searcher = searcher;
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

    public void setOwnBookEnabled(boolean ownBookEnabled) {
        this.config.setOwnBookEnabled(ownBookEnabled);
    }

    public void setOwnTablebaseEnabled(boolean ownTablebaseEnabled) {
        this.config.setOwnTablebaseEnabled(ownTablebaseEnabled);
    }

    public void setPonderEnabled(boolean ponderEnabled) {
        this.config.setPonderEnabled(ponderEnabled);
    }

    public void setSearchCancelled(boolean cancelled) {
        this.config.setSearchCancelled(cancelled);
    }

    public void setPondering(boolean pondering) {
        this.config.setPondering(pondering);
    }

    public void findBestMove(TimeControl tc, Consumer<SearchResult> onThinkComplete) {

        if (useOpeningBook()) {
            // If we are in the opening and book is enabled, select a move from the opening book
            Move bookMove = book.getBookMove(board);
            onThinkComplete.accept(new SearchResult(0, move(bookMove), 0, 0, 0, 0));
        }
        else if (useEndgameTablebase(tc)) {
            // If we are in the endgame and tablebase is enabled, select a move from the tablebase probe
            try {
                Move tablebaseMove = tablebase.getTablebaseMove(board);
                onThinkComplete.accept(new SearchResult(0, move(tablebaseMove), 0, 0, 0, 0));
            } catch (TablebaseException e) {
                // In case tablebase probe fails, search manually
                System.out.println("error probing tablebase: " + e.getMessage());
                startThinking(tc, onThinkComplete);
            }
        }
        else {
            // Otherwise, search for the best move.
            startThinking(tc, onThinkComplete);
        }

    }

    public SearchResult think(int timeout) {
        TimeControl tc = new TimeControl(Duration.ofMillis(timeout), Duration.ofMillis(timeout), -1, -1);
        return searcher.search(tc);
    }

    public SearchResult think(TimeControl tc) {
        return searcher.search(tc);
    }

    public boolean isThinking() {
        return think != null && !think.isDone();
    }

    public void startThinking(TimeControl tc, Consumer<SearchResult> onThinkComplete) {
        stopThinking();
        think = CompletableFuture.supplyAsync(() -> think(tc));
        think.thenAccept(onThinkComplete);
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
        long zobristKey = board.getGameState().getZobrist();
        HashEntry entry = transpositionTable.get(zobristKey, 0);
        board.unmakeMove();
        return entry != null ? entry.getMove() : null;
    }

    public List<Move> extractPrincipalVariation() {
        List<Move> principalVariation = new ArrayList<>();
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        int moveCount = 0;
        while (moveCount <= 12) {
            long zobristKey = board.getGameState().getZobrist();
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

    private boolean useOpeningBook() {
        long key = board.getGameState().getZobrist();
        int moveCount = board.getMoveHistory().size();
        return config.isOwnBookEnabled() && moveCount < config.getOwnBookMaxMoves() && book.hasBookMove(key);
    }

    private boolean useEndgameTablebase(TimeControl tc) {
        return !config.isPondering()
                && config.isOwnTablebaseEnabled()
                && board.countPieces() <= config.getMaxTablebaseSupportedPieces()
                && tablebase.canProbeTablebase(tc.hardLimit());
    }

    /**
     * For moves parsed from UCI, read from the opening book, or probed from the endgame tablebase, generate the
     * corresponding 'legal' move which includes any special move flag (promotion, en passant, castling etc.)
     */
    private Move move(Move move) {
        return moveGenerator.generateMoves(board).stream()
                .filter(move::matches)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Illegal move " + Notation.toNotation(move)));
    }

}
