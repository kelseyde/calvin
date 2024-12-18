package com.kelseyde.calvin.uci;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.search.Score;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.uci.UCICommand.GoCommand;
import com.kelseyde.calvin.uci.UCICommand.PositionCommand;
import com.kelseyde.calvin.uci.UCICommand.ScoreDataCommand;
import com.kelseyde.calvin.utils.Bench;
import com.kelseyde.calvin.utils.notation.FEN;
import com.kelseyde.calvin.utils.train.TrainingDataScorer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Entrypoint for the Calvin chess engine.
 * <p>
 * Calvin communicates using the Universal Chess Interface protocol (UCI). This adapter acts as a UCI interface which
 * allows the user to interact with the {@link Engine}, and provides a way to configure the engine and play games.
 * @see <a href="https://www.chessprogramming.org/UCI">Chess Programming Wiki</a>
 */
public class UCI {

    private static final Engine ENGINE = Engine.getInstance();

    public static class Options {
        public static boolean output = true;
        public static boolean pretty = false;
        public static boolean chess960 = false;
    }

    public static void run(String[] args) {

        // Enable pretty printing if the engine is running in a terminal.
        Options.pretty = System.console() != null;

        writeEngineInfo();

        // Allow the engine to be benched from the command line at startup.
        if (args.length == 1 && args[0].equals("bench")) {
            Bench.run(ENGINE, true);
        }

        try (Scanner in = new Scanner(System.in)) {
            String input = "";
            while (!input.equals("quit")) {
                input = in.nextLine();
                if (!input.isEmpty()) {
                    // Parse the input and execute the command.
                    UCICommand command = UCICommand.parse(input);
                    command.execute();
                }
            }
        } catch (Exception e) {
            writeError("error processing command", e);
        }


    }

    public static void handleUCI(UCICommand command) {
        write("id name Calvin");
        write("id author Dan Kelsey");
        EngineConfig config = ENGINE.getConfig();
        write(String.format("option name Hash type spin default %s min %s max %s",
                config.defaultHashSizeMb, config.minHashSizeMb, config.maxHashSizeMb));
        write(String.format("option name Threads type spin default %s min %s max %s",
                config.defaultThreads, config.minThreads, config.maxThreads));
        write(String.format("option name Ponder type check default %s", config.ponderEnabled));
        write("option name UCI_Chess960 type check default false");
        write("option name Pretty type check default false");
        ENGINE.getConfig().getTunables().forEach(t -> write(t.toUCI()));
        write("uciok");

        // Typically 'uci' is only sent by tournament runners, not humans.
        // Therefore, let's disable pretty print when receiving a 'uci' command.
        Options.pretty = false;
    }

    public static void handleBench(UCICommand command) {
        Bench.run(ENGINE, false);
    }

    public static void handleNewGame(UCICommand command) {
        ENGINE.gameOver();
        System.gc();
        ENGINE.newGame();
    }

    public static void handleIsReady(UCICommand command) {
        write("readyok");
    }

    public static void handlePosition(UCICommand command) {
        PositionCommand positionCommand = PositionCommand.parse(command);
        ENGINE.setPosition(positionCommand);
    }

    public static void handleGo(UCICommand command) {
        GoCommand goCommand = GoCommand.parse(command);
        ENGINE.go(goCommand);
    }

    public static void handlePonderHit(UCICommand command) {
        ENGINE.setPondering(false);
    }

    public static void handleSetOption(UCICommand command) {
        String name = command.getString("name", "", true);
        switch (name) {
            case "Hash":          setHashSize(command); break;
            case "Threads":       setThreads(command); break;
            case "Ponder":        setPonder(command); break;
            case "Pretty":        setPretty(command); break;
            case "UCI_Chess960":  handleChess960(command); break;
            default:              ENGINE.getConfig().setTunable(command); break;
        }
    }

    public static void handleScoreData(UCICommand command) {
        TrainingDataScorer scorer = new TrainingDataScorer();
        ScoreDataCommand.parse(command).ifPresent(scorer::score);
        write("info string score data complete");
    }

    public static void handleHelp(UCICommand command) {
        write("");
        write("the following commands are available:");
        write("uci            -- print engine info");
        write("isready        -- check if engine is ready");
        write("setoption      -- set engine options (type 'uci' to see the available options'");
        write("                   args:");
        write("                       -- name: the name of the option to change");
        write("                       -- value: the new value for the option");
        write("ucinewgame     -- clear the board and set up a new game");
        write("position       -- set up the board position");
        write("                   args:");
        write("                       -- startpos: set up the starting position");
        write("                          OR");
        write("                       -- fen: supply the position in FEN string format");
        write("                       -- moves: the moves played from the supplied position");
        write("go             -- start searching for the best move");
        write("                   args:");
        write("                       -- movetime: the time to spend searching in milliseconds");
        write("                          OR");
        write("                       -- wtime: white time remaining in milliseconds");
        write("                       -- btime: black time remaining in milliseconds");
        write("                       -- winc: white increment in milliseconds");
        write("                       -- binc: black increment in milliseconds");
        write("                       -- nodes: max nodes to search");
        write("                       -- depth: max depth to search");
        write("                          OR ");
        write("                       -- perft <depth>: run a perft test to the specified depth");
        write("stop           -- stop searching and return the best move");
        write("display / d    -- display the current board state");
        write("fen            -- print the FEN string for the current position");
        write("eval           -- evaluate the current position");
        write("pretty         -- toggle pretty console output");
        write("scoredata      -- score a data file with the engine, to train a neural network");
        write("                   args:");
        write("                       -- input: the input file to score");
        write("                       -- output: the output file to write the scores to");
        write("                       -- depth: the depth to search to (default 5000)");
        write("                       -- resume: the line number to resume from (default 0)");
        write("quit           -- exit the application");
        write("");
    }

    public static void handleFen(UCICommand command) {
        if (ENGINE.getBoard() != null) {
            write(FEN.toFEN(ENGINE.getBoard()));
        } else {
            write("info error no position specified, please use the 'position' command first");
        }
    }

    public static void handleDisplay(UCICommand command) {
        ENGINE.getBoard().print();
    }

    public static void handleThreats(UCICommand command) {
        MoveGenerator movegen = new MoveGenerator();
        long threats = movegen.calculateThreats(ENGINE.getBoard(), !ENGINE.getBoard().isWhite());
        Bits.print(threats);
    }

    public static void handleEval(UCICommand command) {
        NNUE nnue = new NNUE(ENGINE.getBoard());
        write(String.valueOf(nnue.evaluate()));
    }

    public static void handleStop(UCICommand command) {
        ENGINE.setPondering(false);
        ENGINE.setSearchCancelled(true);
    }

    public static void handleQuit(UCICommand command) {
        ENGINE.gameOver();
        System.exit(0);
    }

    public static void handleUnknown(UCICommand command) {
        write("info error unknown command " + command.args()[0]);
    }

    public static void setOutputEnabled(boolean output) {
        UCI.Options.output = output;
    }

    private static void setHashSize(UCICommand command) {
        int hashSizeMb = command.getInt("value", -1, true);
        int minHashSizeMb = ENGINE.getConfig().minHashSizeMb;
        int maxHashSizeMb = ENGINE.getConfig().maxHashSizeMb;
        if (hashSizeMb >= minHashSizeMb && hashSizeMb <= maxHashSizeMb) {
            ENGINE.setHashSize(hashSizeMb);
            write("info string Hash " + hashSizeMb);
        } else {
            write(String.format("hash size %s not in valid range %s - %s", hashSizeMb, minHashSizeMb, maxHashSizeMb));
        }
    }

    private static void setThreads(UCICommand command) {
        int threadCount = command.getInt("value", -1, true);
        int minThreadCount = ENGINE.getConfig().minThreads;
        int maxThreadCount = ENGINE.getConfig().maxThreads;
        if (threadCount >= minThreadCount && threadCount <= maxThreadCount) {
            ENGINE.setThreadCount(threadCount);
            write("info string Threads " + threadCount);
        } else {
            write(String.format("thread count %s not in valid range %s - %s", threadCount, minThreadCount, maxThreadCount));
        }
    }

    private static void setPonder(UCICommand command) {
        boolean ponderEnabled = command.getBool("value", false, true);
        ENGINE.setPonderEnabled(ponderEnabled);
        write("info string Ponder " + ponderEnabled);
    }

    private static void setPretty(UCICommand command) {
        boolean prettyEnabled = command.getBool("value", false, true);
        Options.pretty = prettyEnabled;
        write("info string Pretty " + prettyEnabled);
    }

    public static void handleChess960(UCICommand command) {
        boolean chess960Enabled = command.getBool("value", false, true);
        Options.chess960 = chess960Enabled;
        write("info string UCI_Chess960 " + chess960Enabled);
    }

    public static void handlePretty(UCICommand command) {
        Options.pretty = !Options.pretty;
        write("info string Pretty " + Options.pretty);
    }

    public static void handleHashfull(UCICommand command) {
        write(String.format(Locale.ROOT, "%.1f", (float) ENGINE.hashfull() / 1000 * 100));
    }

    public static void writeEngineInfo() {

        if (Options.pretty) {
            Pretty.printEngineInfo();
        } else {
            write("Calvin by Dan Kelsey");
        }

    }

    public static void writeSearchInfo(SearchResult searchResult) {
        int depth = searchResult.depth();
        int seldepth = searchResult.seldepth();
        int score = searchResult.eval();
        long time = searchResult.time();
        int nodes = searchResult.nodes();
        long nps = searchResult.nps();
        int hashfull = ENGINE.hashfull();
        List<Move> pv = ENGINE.extractPrincipalVariation();
        if (Options.pretty) {
            Pretty.writeSearchInfo(depth, seldepth, score, time, nodes, nps, hashfull, pv);
        } else {
            String pvString = pv.stream().map(Move::toUCI).collect(Collectors.joining(" "));
            write(String.format("info depth %s seldepth %s score %s nodes %s time %s nps %s hashfull %s pv %s",
                    depth, seldepth, formatScore(score), nodes, time, nps, hashfull, pvString));
        }
    }

    private static String formatScore(int eval) {
        if (Score.isMateScore(eval)) {
            int moves = Math.max((Score.MATE - Math.abs(eval)) / 2, 1);
            if (eval < 0) moves = -moves;
            return "mate " + moves;
        } else {
            return "cp " + eval;
        }
    }

    public static void writeMove(SearchResult searchResult) {
        Move move = searchResult.move();
        boolean ponderEnabled = ENGINE.getConfig().ponderEnabled;
        if (ponderEnabled && move != null) {
            Move ponderMove = ENGINE.extractPonderMove(move);
            write(String.format("bestmove %s ponder %s", Move.toUCI(move), Move.toUCI(ponderMove)));
        } else {
            write(String.format("bestmove %s", Move.toUCI(move)));
        }
    }

    public static void write(String output) {
        if (Options.output) System.out.println(output);
    }

    public static void writeError(String output, Exception e) {
        write("info error " + output + " " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
    }

}
