package com.kelseyde.calvin.uci;

import java.util.Arrays;
import java.util.function.Consumer;

enum UCICommandType {

    UCI_INFO     ("uci",         UCI::handleUCI),
    IS_READY     ("isready",     UCI::handleIsReady),
    HELP         ("help",        UCI::handleHelp),
    SET_OPTION   ("setoption",   UCI::handleSetOption),
    UCI_NEWGAME  ("ucinewgame",  UCI::handleNewGame),
    POSITION     ("position",    UCI::handlePosition),
    GO           ("go",          UCI::handleGo),
    PONDERHIT    ("ponderhit",   UCI::handlePonderHit),
    FEN          ("fen",         UCI::handleFen),
    EVAL         ("eval",        UCI::handleEval),
    BENCH        ("bench",       UCI::handleBench),
    SCORE_DATA   ("scoredata",   UCI::handleScoreData),
    STOP         ("stop",        UCI::handleStop),
    QUIT         ("quit",        UCI::handleQuit),
    UNKNOWN      ("unknown",     UCI::handleUnknown);

    final String name;
    final CommandConsumer consumer;

    UCICommandType(String name, CommandConsumer consumer) {
        this.name = name;
        this.consumer = consumer;
    }

    public static UCICommandType parse(String command) {
        final String label = command.trim().toLowerCase().split(" ")[0];
        return Arrays.stream(UCICommandType.values())
                .filter(type -> type.name.equals(label))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public interface CommandConsumer extends Consumer<UCICommand> {

        @Override
        default void accept(UCICommand command) {
            try {
               acceptThrows(command);
            } catch (Exception e) {
                UCI.writeError("error processing command", e);
            }
        }

        void acceptThrows(UCICommand command) throws Exception;

    }

}
