package com.kelseyde.calvin;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.evaluation.Score;
import com.kelseyde.calvin.search.SearchResult;
import com.kelseyde.calvin.search.TimeControl;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import com.kelseyde.calvin.utils.train.TrainingDataScorer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Application {

    public static final Engine ENGINE = EngineInitializer.loadEngine();
    static final Scanner SCANNER = new Scanner(System.in);
    static final String[] POSITION_LABELS = new String[] { "position", "fen", "moves" };
    static final String[] GO_LABELS = new String[] { "go", "movetime", "wtime", "btime", "winc", "binc", "movestogo" };
    static final String[] SETOPTION_LABELS = new String[] { "setoption", "name", "value" };
    static final int DEFAULT_NODE_SOFT_LIMIT = 5000;
    public static boolean outputEnabled = true;

    public static void main(String[] args) {
        try {
            String command = "";
            while (!command.equals("quit")) {
                command = readCommand();
                if (!command.isEmpty()) {
                    String commandType = command.trim().toLowerCase().split(" ")[0];
                    switch (commandType) {
                        case "uci" ->         handleUci();
                        case "isready" ->     handleIsReady();
                        case "help" ->        handleHelp();
                        case "setoption" ->   handleSetOption(command);
                        case "ucinewgame" ->  handleNewGame();
                        case "position" ->    handlePosition(command);
                        case "go" ->          handleGo(command);
                        case "ponderhit" ->   handlePonderHit();
                        case "fen" ->         handleFen();
                        case "eval" ->        handleEval();
                        case "scoredata" ->   handleScoreData(command);
                        case "stop" ->        handleStop();
                        case "quit" ->        handleQuit();
                        default ->            write("Unrecognised command: " + command);
                    }
                }
            }
        } catch (Exception e) {
            write("info error " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
        }
    }

    private static void handleUci() {
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

    private static void handleHelp() {
        write("the following commands are available:");
        write("uci         -- print engine info");
        write("isready     -- check if engine is ready");
        write("setoption   -- set engine options (type 'uci' to see the available options'");
        write("                args:");
        write("                -- name: the name of the option to change");
        write("                -- value: the new value for the option");
        write("ucinewgame  -- clear the board and set up a new game");
        write("position    -- set up the board position");
        write("                args:");
        write("                -- startpos: set up the starting position");
        write("                   OR");
        write("                -- fen: supply the position in FEN string format");
        write("                -- moves: the moves played from the supplied position");
        write("go          -- start searching for the best move");
        write("                args:");
        write("                -- movetime: the time to spend searching in milliseconds");
        write("                   OR");
        write("                -- wtime: white time remaining in milliseconds");
        write("                -- btime: black time remaining in milliseconds");
        write("                -- winc: white increment in milliseconds");
        write("                -- binc: black increment in milliseconds");
        write("stop        -- stop searching and return the best move");
        write("fen         -- print the FEN string for the current position");
        write("eval        -- evaluate the current position");
        write("datagen     -- generate training data for neural network");
        write("quit        -- exit the application");
    }

    private static void handleIsReady() {
        write("readyok");
    }

    private static void handleSetOption(String command) {
        String optionType = getLabelString(command, "name", SETOPTION_LABELS, "");
        switch (optionType) {
            case "Hash":          setHashSize(command); break;
            case "Threads":       setThreadCount(command); break;
            case "OwnBook":       setOwnBook(command); break;
            case "OwnTablebase":  setOwnTablebase(command); break;
            case "Ponder":        setPonder(command); break;
            default:              write("unrecognised option name " + optionType);
        }
    }

    private static void handleNewGame() {
        ENGINE.gameOver();
        ENGINE.newGame();
    }

    private static void handlePosition(String command) {
        String fen;
        if (command.contains("startpos")) {
            fen = FEN.STARTING_POSITION;
        }
        else if (command.contains("fen")) {
            fen = getLabelString(command, "fen", POSITION_LABELS, "");
        }
        else {
            write("Invalid position command, expecting 'position' or 'fen'.");
            return;
        }
        List<Move> moves = new ArrayList<>();
        if (command.contains("moves")) {
            String moveString = getLabelString(command, "moves", POSITION_LABELS, "");
            if (!moveString.isBlank()) {
                moves.addAll(new ArrayList<>(Arrays.stream(moveString.split(" "))
                        .map(Notation::fromCombinedNotation)
                        .toList()));
            }
            else {
                write("Invalid position command, 'moves' has no content.");
                return;
            }
        }
        ENGINE.setPosition(fen, moves);

    }

    private static void handleGo(String command) {

        boolean ponder = command.contains("ponder");
        ENGINE.setPondering(ponder);
        ENGINE.setSearchCancelled(false);

        if ((command.contains("wtime") && !command.contains("btime")) ||
                !command.contains("wtime") && command.contains("btime")) {
            write("info error both wtime and btime must be specified");
            return;
        }

        TimeControl tc;
        if (command.contains("movetime")) {
            Duration thinkTime = Duration.ofMillis(getLabelInt(command, "movetime", GO_LABELS));
            tc = new TimeControl(thinkTime, thinkTime);
        }
        else if (command.contains("wtime")) {
            int timeWhiteMs = getLabelInt(command, "wtime", GO_LABELS);
            int timeBlackMs = getLabelInt(command, "btime", GO_LABELS);
            int incrementWhiteMs = getLabelInt(command, "winc", GO_LABELS);
            int incrementBlackMs = getLabelInt(command, "binc", GO_LABELS);
            tc = TimeControl.init(ENGINE.getBoard(), timeWhiteMs, timeBlackMs, incrementWhiteMs, incrementBlackMs);
        }
        else {
            Duration thinkTime = Duration.ofMillis(Integer.MAX_VALUE);
            tc = new TimeControl(thinkTime, thinkTime);
        }
        ENGINE.findBestMove(tc, Application::writeMove);
    }

    private static void handleEval() {

        NNUE nnue = new NNUE(ENGINE.getBoard());
        write(String.valueOf(nnue.evaluate()));

    }

    private static void handleFen() {
        if (ENGINE.getBoard() != null) {
            write(FEN.toFEN(ENGINE.getBoard()));
        } else {
            write("info error no position specified, please use the 'position' command first");
        }
    }

    private static void handlePonderHit() {
        ENGINE.setPondering(false);
    }

    private static void handleScoreData(String command) {
        String[] parts = command.split(" ");
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
        int softNodeLimit = DEFAULT_NODE_SOFT_LIMIT;
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
            write("info error " + e.getMessage());
        }
        write("info string score data complete");
    }

    private static void handleStop() {
        ENGINE.setPondering(false);
        ENGINE.setSearchCancelled(true);
    }

    private static void handleQuit() {
        ENGINE.gameOver();
        System.exit(0);
    }

    public static void writeSearchInfo(SearchResult searchResult) {
        if (!outputEnabled) {
            return;
        }
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

    private static void setHashSize(String command) {
        int hashSizeMb = getLabelInt(command, "value", SETOPTION_LABELS);
        int minHashSizeMb = ENGINE.getConfig().getMinHashSizeMb();
        int maxHashSizeMb = ENGINE.getConfig().getMaxHashSizeMb();
        if (hashSizeMb >= minHashSizeMb && hashSizeMb <= maxHashSizeMb) {
            ENGINE.setHashSize(hashSizeMb);
            write("info string Hash " + hashSizeMb);
        } else {
            write(String.format("hash size %s not in valid range %s - %s", hashSizeMb, minHashSizeMb, maxHashSizeMb));
        }
    }

    private static void setThreadCount(String command) {
        int threadCount = getLabelInt(command, "value", SETOPTION_LABELS);
        int minThreadCount = ENGINE.getConfig().getMinThreadCount();
        int maxThreadCount = ENGINE.getConfig().getMaxThreadCount();
        if (threadCount >= minThreadCount && threadCount <= maxThreadCount) {
            ENGINE.setThreadCount(threadCount);
            write("info string Threads " + threadCount);
        } else {
            write(String.format("thread count %s not in valid range %s - %s", threadCount, minThreadCount, maxThreadCount));
        }
    }

    private static void setOwnBook(String command) {
        boolean ownBookEnabled = Boolean.parseBoolean(getLabelString(command, "value", SETOPTION_LABELS, "false"));
        ENGINE.setOwnBookEnabled(ownBookEnabled);
        write("info string OwnBook " + ownBookEnabled);
    }

    private static void setOwnTablebase(String command) {
        boolean ownTablebaseEnabled = Boolean.parseBoolean(getLabelString(command, "value", SETOPTION_LABELS, "false"));
        ENGINE.setOwnTablebaseEnabled(ownTablebaseEnabled);
        write("info string OwnTablebase " + ownTablebaseEnabled);
    }

    private static void setPonder(String command) {
        boolean ponderEnabled = Boolean.parseBoolean(getLabelString(command, "value", SETOPTION_LABELS, "false"));
        ENGINE.setPonderEnabled(ponderEnabled);
        write("info string Ponder " + ponderEnabled);
    }

    private static String getLabelString(String command, String label, String[] allLabels, String defaultValue) {
        if (command.contains(label)) {
            int valueStart = command.indexOf(label) + label.length();
            int valueEnd = command.length();
            for (String otherLabel : allLabels) {
                if (!otherLabel.equals(label) && command.contains(label)) {
                    int otherValueStart = command.indexOf(otherLabel);
                    if (otherValueStart > valueStart && otherValueStart < valueEnd) {
                        valueEnd = otherValueStart;
                    }
                }
            }
            return command.substring(valueStart, valueEnd).trim();
        }
        return defaultValue;
    }

    private static int getLabelInt(String command, String label, String[] allLabels) {
        String valueString = getLabelString(command, label, allLabels, String.valueOf(0));
        return !valueString.isBlank() ? Integer.parseInt(valueString) : 0;
    }

    public static String readCommand() {
        try {
            String line = SCANNER.nextLine();
            if (!line.isBlank()) {
                return line;
            }
        } catch (NoSuchElementException e) {
            // do nothing
        }
        return "";
    }

    private static void write(String output) {
        if (outputEnabled) System.out.println(output);
    }

}
