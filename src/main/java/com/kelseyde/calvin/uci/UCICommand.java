package com.kelseyde.calvin.uci;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record UCICommand(UCICommandType type, String[] args) {

    public static UCICommand parse(String input) {
        String[] args = input.trim().split(" ");
        UCICommandType type = UCICommandType.parse(args[0]);
        return new UCICommand(type, args);
    }

    public String getString(String label, String defaultValue, boolean panic) {
        int labelIndex = Arrays.asList(args).indexOf(label);
        if (labelIndex == -1) {
            if (panic) throw new IllegalArgumentException("missing required label " + label);
            return defaultValue;
        }
        int valueIndex = labelIndex + 1;
        if (valueIndex >= args.length) {
            if (panic) throw new IllegalArgumentException("missing value for label " + label);
            return defaultValue;
        }
        return args[valueIndex];
    }

    public List<String> getStrings(String label, boolean panic) {
        int labelIndex = Arrays.asList(args).indexOf(label);
        if (labelIndex == -1) {
            if (panic) throw new IllegalArgumentException("missing required label " + label);
            return new ArrayList<>();
        }
        int valueIndex = labelIndex + 1;
        if (valueIndex >= args.length) {
            if (panic) throw new IllegalArgumentException("missing value for label " + label);
            return new ArrayList<>();
        }
        return Arrays.asList(args).subList(valueIndex, args.length);
    }

    public int getInt(String label, int defaultValue, boolean panic) {
        String valueString = getString(label, String.valueOf(defaultValue), panic);
        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            if (panic) throw new IllegalArgumentException("invalid value for label " + label + ": " + valueString);
            return defaultValue;
        }
    }

    public boolean getBool(String label, boolean defaultValue, boolean panic) {
        String valueString = getString(label, String.valueOf(defaultValue), panic);
        return Boolean.parseBoolean(valueString);
    }

    public void execute() {
        type.consumer.accept(this);
    }

    public boolean contains(String label) {
        return Arrays.asList(args).contains(label);
    }

    public record GoCommand(int movetime, int wtime, int btime, int winc, int binc, int nodes, int depth) {

        public static GoCommand parse(UCICommand command) {
            int movetime = command.getInt("movetime", -1, false);
            int wtime = command.getInt("wtime", -1, false);
            int btime = command.getInt("btime", -1, false);
            int winc = command.getInt("winc", -1, false);
            int binc = command.getInt("binc", -1, false);
            int nodes = command.getInt("nodes", -1, false);
            int depth = command.getInt("depth", -1, false);
            return new GoCommand(movetime, wtime, btime, winc, binc, nodes, depth);
        }

        public boolean isMovetime() {
            return movetime > 0;
        }

        public boolean isTime() {
            return wtime > 0 && btime > 0;
        }

    }

    public record PositionCommand(String fen, List<Move> moves) {

        public static PositionCommand parse(UCICommand command) {
            String fen;
            if (command.contains("startpos")) {
                fen = FEN.STARTPOS;
            } else if (command.contains("fen")) {
                fen = String.join(" ", command.getStrings("fen", true));
            } else {
                UCI.write("info error invalid position command; expecting 'startpos' or 'fen'.");
                fen = FEN.STARTPOS;
            }
            List<Move> moves = command.getStrings("moves", false).stream()
                    .map(Notation::fromUCI)
                    .toList();
            return new PositionCommand(fen, moves);
        }

    }

    public record ScoreDataCommand(String inputFile, String outputFile, int softNodeLimit, int resumeOffset) {

        public static Optional<ScoreDataCommand> parse(UCICommand command) {
            String[] parts = command.args();
            if (parts.length < 3) {
                UCI.write("info error invalid command; input and output file must be specified; e.g. 'scoredata input.txt output.txt'");
                return Optional.empty();
            }
            String inputFile = parts[1];
            String outputFile = parts[2];
            if (!Files.exists(Path.of(inputFile))) {
                UCI.write("info error input file " + inputFile + " does not exist");
                return Optional.empty();
            }
            int softNodeLimit = 5000;
            int resumeOffset = 0;
            if (parts.length > 3) {
                try {
                    softNodeLimit = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    UCI.write("info error invalid depth; must be an integer; e.g. 'scoredata input.txt output.txt 6'");
                    return Optional.empty();
                }
                if (parts.length > 4) {
                    try {
                        resumeOffset = Integer.parseInt(parts[4]);
                    } catch (NumberFormatException e) {
                        UCI.write("info error invalid resume offset; must be an integer; e.g. 'scoredata input.txt output.txt 6 1000'");
                        return Optional.empty();
                    }
                }
            }
            return Optional.of(new ScoreDataCommand(inputFile, outputFile, softNodeLimit, resumeOffset));
        }

    }

}
