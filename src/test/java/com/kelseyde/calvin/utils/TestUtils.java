package com.kelseyde.calvin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.endgame.lichess.LichessTablebase;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.Evaluator;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.ParallelSearcher;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.Searcher;
import com.kelseyde.calvin.search.ThreadManager;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.transposition.TranspositionTable;
import com.kelseyde.calvin.utils.notation.Notation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TestUtils {

    public static final String PRD_CONFIG_LOCATION = "src/main/resources/engine_config.json";
    public static final String TST_CONFIG_LOCATION = "src/test/resources/engine_config.json";
    public static final EngineConfig PRD_CONFIG = loadConfig(PRD_CONFIG_LOCATION);
    public static final EngineConfig TST_CONFIG = loadConfig(TST_CONFIG_LOCATION);
    public static final OpeningBook OPENING_BOOK = EngineInitializer.loadDefaultOpeningBook();
    public static final Tablebase TABLEBASE = new LichessTablebase(PRD_CONFIG);
    public static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();
    public static final MoveOrdering MOVE_ORDERER = new MoveOrderer();
    public static final Evaluation EVALUATOR = new Evaluator(PRD_CONFIG);
    public static final TranspositionTable TRANSPOSITION_TABLE = new TranspositionTable(PRD_CONFIG.getDefaultHashSizeMb());
    public static final ThreadManager THREAD_MANAGER = new ThreadManager();
    public static final Searcher SEARCHER = new Searcher(PRD_CONFIG, THREAD_MANAGER, MOVE_GENERATOR, MOVE_ORDERER, EVALUATOR, TRANSPOSITION_TABLE);
    public static final Search PARALLEL_SEARCHER = new ParallelSearcher(PRD_CONFIG, MoveGenerator::new, MoveOrderer::new, () -> new Evaluator(PRD_CONFIG), TRANSPOSITION_TABLE);
    public static final String QUIET_POSITIONS_FILE = "src/test/resources/texel/quiet_positions.epd";
    public static final String QUIET_POSITIONS_EXTENDED_FILE = "src/test/resources/texel/quiet_positions_extended.epd";
    public static final String QUIET_LABELLED_V6_FILE = "src/test/resources/texel/quiet-labeled.v6.epd";
    public static final String QUIET_LABELLED_V7_FILE = "src/test/resources/texel/quiet-labeled.v7.epd";

    public static Engine getEngine() {
        return new Engine(PRD_CONFIG, OPENING_BOOK, TABLEBASE, new MoveGenerator(), new Searcher(PRD_CONFIG, new ThreadManager(), new MoveGenerator(), new MoveOrderer(), new Evaluator(PRD_CONFIG), new TranspositionTable(PRD_CONFIG.getDefaultHashSizeMb())));
    }

    private static EngineConfig loadConfig(String configLocation) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Path path = Paths.get(configLocation);
            String json = Files.readString(path);
            EngineConfig config = mapper.readValue(json, EngineConfig.class);
            config.postInitialise();
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Board emptyBoard() {
        Board board = new Board();
        board.setWhitePawns(0L);
        board.setWhiteKnights(0L);
        board.setWhiteBishops(0L);
        board.setWhiteRooks(0L);
        board.setWhiteQueens(0L);
        board.setWhiteKing(0L);

        board.setBlackPawns(0L);
        board.setBlackKnights(0L);
        board.setBlackBishops(0L);
        board.setBlackRooks(0L);
        board.setBlackQueens(0L);
        board.setBlackKing(0L);
        board.setPieceList(new Piece[64]);

        board.getGameState().setCastlingRights(0b0000);

        board.recalculatePieces();

        return board;
    }

    public static Move getLegalMove(Board board, String startSquare, String endSquare) {
        Move move = Notation.fromNotation(startSquare, endSquare);
        List<Move> legalMoves = MOVE_GENERATOR.generateMoves(board);
        Optional<Move> legalMove = legalMoves.stream()
                .filter(m -> m.matches(move))
                .findAny();
        if (legalMove.isEmpty()) {
            throw new IllegalMoveException(String.format("Illegal move! %s%s", startSquare, endSquare));
        }
        return legalMove.get();
    }

    public static Move getLegalMove(Board board, Move move) {
        List<Move> legalMoves = MOVE_GENERATOR.generateMoves(board);
        Optional<Move> legalMove = legalMoves.stream()
                .filter(m -> m.matches(move))
                .findAny();
        if (legalMove.isEmpty()) {
            throw new IllegalMoveException(String.format("Illegal move! %s", move));
        }
        return legalMove.get();
    }

    public static List<String> loadFens() throws IOException {
        Path path = Paths.get(QUIET_POSITIONS_FILE);
        return Files.readAllLines(path);
    }

    public static List<String> loadAllFens() throws IOException {
        Path path = Paths.get(QUIET_LABELLED_V7_FILE);
        List<String> firstFens = new ArrayList<>();
        path = Paths.get(QUIET_LABELLED_V7_FILE);
        List<String> secondFends = Files.readAllLines(path);
        firstFens.addAll(secondFends);
        return firstFens;
    }

}
