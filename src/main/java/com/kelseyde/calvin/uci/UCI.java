package com.kelseyde.calvin.uci;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.evaluation.Score;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.search.TimeControl;
import com.kelseyde.calvin.uci.UCICommand.GoCommand;
import com.kelseyde.calvin.utils.Bench;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import com.kelseyde.calvin.utils.train.TrainingDataScorer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entrypoint for the Calvin chess engine. Calvin communicates using the Universal Chess Interface protocol (UCI).
 * This adapter acts as a UCI interface which translates the incoming commands to instructions for the {@link Engine},
 * which is responsible for actually playing the game of chess.
 * @see <a href="https://www.chessprogramming.org/UCI">Chess Programming Wiki</a>
 */
public class UCI {

    public static final Engine ENGINE = EngineInitializer.loadEngine();
    static final Scanner READER = new Scanner(System.in);
    static final Bench BENCH = new Bench();
    public static boolean outputEnabled = true;

    public static void run(String[] args) {

        if (args[0] != null && args[0].equals("bench")) {
            BENCH.run();
        }

        try {
            String input = "";
            while (!input.equals("quit")) {
                input = READER.nextLine();
                if (!input.isEmpty()) {
                    UCICommand command = UCICommand.parse(input);
                    command.execute();
                }
            }
        } catch (Exception e) {
            writeError("error processing command", e);
        }
    }

    // TODO move options to EngineConfig
    public static void handleUCI(UCICommand command) {
        write("id name Calvin");
        write("id author Dan Kelsey");
        EngineConfig config = ENGINE.getConfig();
        write(String.format("option name Hash type spin default %s min %s max %s",
                config.getDefaultHashSizeMb(), config.getMinHashSizeMb(), config.getMaxHashSizeMb()));
        write(String.format("option name Threads type spin default %s min %s max %s",
                config.getDefaultThreadCount(), config.getMinThreadCount(), config.getMaxThreadCount()));
        write(String.format("option name OwnBook type check default %s", config.isOwnBookEnabled()));
        write(String.format("option name OwnTablebase type check default %s", config.isOwnTablebaseEnabled()));
        write(String.format("option name Ponder type check default %s", config.isPonderEnabled()));
        write("uciok");
    }

    public static void handleBench(UCICommand command) {
        BENCH.run();
    }

    public static void handleNewGame(UCICommand command) {
        ENGINE.gameOver();
        ENGINE.newGame();
    }

    public static void handleIsReady(UCICommand command) {
        write("readyok");
    }

    public static void handlePosition(UCICommand command) {
        String fen;
        if (command.contains("startpos")) {
            fen = FEN.STARTING_POSITION;
        }
        else if (command.contains("fen")) {
            fen = String.join(" ", command.getStrings("fen", true));
        }
        else {
            write("info error invalid position command; expecting 'startpos' or 'fen'.");
            return;
        }
        List<Move> moves = command.getStrings("moves", false).stream()
                .map(Notation::fromCombinedNotation)
                .toList();
        ENGINE.setPosition(fen, moves);

    }

    public static void handleGo(UCICommand command) {
        boolean ponder = command.contains("ponder");
        ENGINE.setPondering(ponder);
        ENGINE.setSearchCancelled(false);
        GoCommand go = GoCommand.parse(command);
        TimeControl tc = TimeControl.init(ENGINE.getBoard(), go);
        ENGINE.findBestMove(tc, UCI::writeMove);
    }

    public static void handlePonderHit(UCICommand command) {
        ENGINE.setPondering(false);
    }

    public static void handleSetOption(UCICommand command) {
        String optionType = command.getString("name", "", true);
        switch (optionType) {
            case "Hash":          setHashSize(command); break;
            case "Threads":       setThreadCount(command); break;
            case "OwnBook":       setOwnBook(command); break;
            case "OwnTablebase":  setOwnTablebase(command); break;
            case "Ponder":        setPonder(command); break;
            default:              write("unrecognised option name " + optionType);
        }
    }

    public static void handleScoreData(UCICommand command) {
        String[] parts = command.args();
        if (parts.length < 3) {
            write("info error invalid command; input and output file must be specified; e.g. 'scoredata input.txt output.txt'");
            return;
        }
        String inputFile = parts[1];
        String outputFile = parts[2];
        if (!Files.exists(Path.of(inputFile))) {
            write("info error input file " + inputFile + " does not exist");
            return;
        }
        int softNodeLimit = 5000;
        int resumeOffset = 0;
        if (parts.length > 3) {
            try {
                softNodeLimit = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                write("info error invalid depth; must be an integer; e.g. 'scoredata input.txt output.txt 6'");
                return;
            }
            if (parts.length > 4) {
                try {
                    resumeOffset = Integer.parseInt(parts[4]);
                } catch (NumberFormatException e) {
                    write("info error invalid resume offset; must be an integer; e.g. 'scoredata input.txt output.txt 6 1000'");
                    return;
                }
            }
        }
        try {
            TrainingDataScorer scorer = new TrainingDataScorer();
            scorer.score(inputFile, outputFile, softNodeLimit, resumeOffset);
        } catch (Exception e) {
            writeError("error scoring data", e);
        }
        write("info string score data complete");
    }

    public static void handleHelp(UCICommand command) {
        write("");
        write("the following commands are available:");
        write("uci         -- print engine info");
        write("isready     -- check if engine is ready");
        write("setoption   -- set engine options (type 'uci' to see the available options'");
        write("                args:");
        write("                    -- name: the name of the option to change");
        write("                    -- value: the new value for the option");
        write("ucinewgame  -- clear the board and set up a new game");
        write("position    -- set up the board position");
        write("                args:");
        write("                    -- startpos: set up the starting position");
        write("                       OR");
        write("                    -- fen: supply the position in FEN string format");
        write("                    -- moves: the moves played from the supplied position");
        write("go          -- start searching for the best move");
        write("                args:");
        write("                    -- movetime: the time to spend searching in milliseconds");
        write("                       OR");
        write("                    -- wtime: white time remaining in milliseconds");
        write("                    -- btime: black time remaining in milliseconds");
        write("                    -- winc: white increment in milliseconds");
        write("                    -- binc: black increment in milliseconds");
        write("                    -- nodes: max nodes to search");
        write("                    -- depth: max depth to search");
        write("stop        -- stop searching and return the best move");
        write("fen         -- print the FEN string for the current position");
        write("eval        -- evaluate the current position");
        write("scoredata   -- score a data file with the engine, to train a neural network");
        write("                args:");
        write("                    -- input: the input file to score");
        write("                    -- output: the output file to write the scores to");
        write("                    -- depth: the depth to search to (default 5000)");
        write("                    -- resume: the line number to resume from (default 0)");
        write("quit        -- exit the application");
        write("");
    }

    public static void handleFen(UCICommand command) {
        if (ENGINE.getBoard() != null) {
            write(FEN.toFEN(ENGINE.getBoard()));
        } else {
            write("info error no position specified, please use the 'position' command first");
        }
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

    public static void writeSearchInfo(SearchResult searchResult) {
        int depth = searchResult.depth();
        String score = formatScore(searchResult.eval());
        long time = searchResult.time();
        int nodes = searchResult.nodes();
        long nps = searchResult.nps();
        String pv = ENGINE.extractPrincipalVariation().stream()
                .map(Notation::toNotation).collect(Collectors.joining(" "));
        write(String.format("info depth %s score %s nodes %s time %s nps %s pv %s", depth, score, nodes, time, nps, pv));
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
        Move ponderMove = ENGINE.extractPonderMove(move);
        boolean ponder = ENGINE.getConfig().isPonderEnabled() && ponderMove != null;
        String message = ponder ?
                String.format("bestmove %s ponder %s", Notation.toNotation(move), Notation.toNotation(ponderMove)) :
                String.format("bestmove %s", Notation.toNotation(move));
        write(message);
    }

    private static void setHashSize(UCICommand command) {
        int hashSizeMb = command.getInt("value", -1, true);
        int minHashSizeMb = ENGINE.getConfig().getMinHashSizeMb();
        int maxHashSizeMb = ENGINE.getConfig().getMaxHashSizeMb();
        if (hashSizeMb >= minHashSizeMb && hashSizeMb <= maxHashSizeMb) {
            ENGINE.setHashSize(hashSizeMb);
            write("info string Hash " + hashSizeMb);
        } else {
            write(String.format("hash size %s not in valid range %s - %s", hashSizeMb, minHashSizeMb, maxHashSizeMb));
        }
    }

    private static void setThreadCount(UCICommand command) {
        int threadCount = command.getInt("value", -1, true);
        int minThreadCount = ENGINE.getConfig().getMinThreadCount();
        int maxThreadCount = ENGINE.getConfig().getMaxThreadCount();
        if (threadCount >= minThreadCount && threadCount <= maxThreadCount) {
            ENGINE.setThreadCount(threadCount);
            write("info string Threads " + threadCount);
        } else {
            write(String.format("thread count %s not in valid range %s - %s", threadCount, minThreadCount, maxThreadCount));
        }
    }

    private static void setOwnBook(UCICommand command) {
        boolean ownBookEnabled = command.getBool("value", false, true);
        ENGINE.setOwnBookEnabled(ownBookEnabled);
        write("info string OwnBook " + ownBookEnabled);
    }

    private static void setOwnTablebase(UCICommand command) {
        boolean ownTablebaseEnabled = command.getBool("value", false, true);
        ENGINE.setOwnTablebaseEnabled(ownTablebaseEnabled);
        write("info string OwnTablebase " + ownTablebaseEnabled);
    }

    private static void setPonder(UCICommand command) {
        boolean ponderEnabled = command.getBool("value", false, true);
        ENGINE.setPonderEnabled(ponderEnabled);
        write("info string Ponder " + ponderEnabled);
    }

    public static void write(String output) {
        if (outputEnabled) System.out.println(output);
    }

    public static void writeError(String output, Exception e) {
        write("info error " + output + " " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
    }

}
