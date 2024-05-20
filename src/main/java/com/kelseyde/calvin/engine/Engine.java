package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.endgame.TablebaseException;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.transposition.HashEntry;
import com.kelseyde.calvin.transposition.TranspositionTable;
import com.kelseyde.calvin.utils.notation.FEN;
import com.kelseyde.calvin.utils.notation.Notation;
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

    public void setPosition(String fen, List<Move> moves) {
        board = FEN.toBoard(fen);
        for (Move move : moves) {
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

    public void findBestMove(int thinkTimeMs, Consumer<SearchResult> onThinkComplete) {

        if (useOpeningBook()) {
            // If we are in the opening and book is enabled, select a move from the opening book
            Move bookMove = book.getBookMove(board);
            onThinkComplete.accept(new SearchResult(0, move(bookMove), 0, 0, 0, 0));
        }
        else if (useEndgameTablebase(thinkTimeMs)) {
            // If we are in the endgame and tablebase is enabled, select a move from the tablebase probe
            try {
                Move tablebaseMove = tablebase.getTablebaseMove(board);
                onThinkComplete.accept(new SearchResult(0, move(tablebaseMove), 0, 0, 0, 0));
            } catch (TablebaseException e) {
                // In case tablebase probe fails, search manually
                System.out.println("error probing tablebase: " + e.getMessage());
                startThinking(thinkTimeMs, onThinkComplete);
            }
        }
        else {
            // Otherwise, search for the best move.
            startThinking(thinkTimeMs, onThinkComplete);
        }

    }

    public SearchResult think(int thinkTimeMs) {
        Duration thinkTime = Duration.ofMillis(thinkTimeMs);
        if (board == null) {
            System.out.println("huh?");
        }
        return searcher.search(thinkTime);
    }

    public boolean isThinking() {
        return think != null && !think.isDone();
    }

    public void startThinking(int thinkTimeMs, Consumer<SearchResult> onThinkComplete) {
        stopThinking();
        think = CompletableFuture.supplyAsync(() -> think(thinkTimeMs));
        think.thenAccept(onThinkComplete);
    }

    public void stopThinking() {
        if (isThinking()) {
            think.cancel(true);
        }
    }

    public int chooseThinkTime(int timeWhiteMs, int timeBlackMs, int incrementWhiteMs, int incrementBlackMs) {

        boolean white = board.isWhiteToMove();
        int timeRemainingMs = white ? timeWhiteMs : timeBlackMs;
        int incrementMs = white ? incrementWhiteMs : incrementBlackMs;

        int overhead = 50;
        timeRemainingMs -= overhead;
        double optimalThinkTime = Math.min(timeRemainingMs * 0.5, timeRemainingMs * 0.03333 + incrementMs);
        double minThinkTime = Math.min(50, (int) (timeRemainingMs * 0.25));
        double thinkTime = Math.max(optimalThinkTime, minThinkTime);
        return (int) thinkTime;

    }

    public void gameOver() {
        stopThinking();
        board = null;
    }

    public Move extractPonderMove(Move bestMove) {
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        board.makeMove(bestMove);
        long zobristKey = board.getGameState().getZobristKey();
        HashEntry entry = transpositionTable.get(zobristKey, 0);
        board.unmakeMove();
        return entry != null ? entry.getMove() : null;
    }

    public List<Move> extractPrincipalVariation() {
        List<Move> principalVariation = new ArrayList<>();
        TranspositionTable transpositionTable = searcher.getTranspositionTable();
        int moveCount = 0;
        while (moveCount <= 12) {
            long zobristKey = board.getGameState().getZobristKey();
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
        long key = board.getGameState().getZobristKey();
        int moveCount = board.getMoveHistory().size();
        return config.isOwnBookEnabled() && moveCount < config.getMaxBookMoves() && book.hasBookMove(key);
    }

    private boolean useEndgameTablebase(int thinkTimeMs) {
        return !config.isPondering()
                && config.isOwnTablebaseEnabled()
                && board.countPieces() <= config.getMaxTablebaseSupportedPieces()
                && tablebase.canProbeTablebase(thinkTimeMs);
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
