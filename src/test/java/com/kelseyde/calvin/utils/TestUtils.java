package com.kelseyde.calvin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.endgame.LichessTablebase;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.ParallelSearcher;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.Searcher;
import com.kelseyde.calvin.search.ThreadManager;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.tables.tt.TranspositionTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class TestUtils {

    public static final String PRD_CONFIG_LOCATION = "src/main/resources/engine_config.json";
    public static final String TST_CONFIG_LOCATION = "src/test/resources/engine_config.json";
    public static final EngineConfig PRD_CONFIG = loadConfig(PRD_CONFIG_LOCATION);
    public static final EngineConfig TST_CONFIG = loadConfig(TST_CONFIG_LOCATION);
    public static final OpeningBook OPENING_BOOK = EngineInitializer.loadDefaultOpeningBook(PRD_CONFIG);
    public static final Tablebase TABLEBASE = new LichessTablebase(PRD_CONFIG);
    public static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();
    public static final MoveOrdering MOVE_ORDERER = new MoveOrderer();
    public static final Evaluation EVALUATOR = new NNUE();
    public static final TranspositionTable TRANSPOSITION_TABLE = new TranspositionTable(PRD_CONFIG.getDefaultHashSizeMb());
    public static final ThreadManager THREAD_MANAGER = new ThreadManager();
    public static final Searcher SEARCHER = new Searcher(TST_CONFIG, THREAD_MANAGER, MOVE_GENERATOR, MOVE_ORDERER, EVALUATOR, TRANSPOSITION_TABLE);
    public static final Search PARALLEL_SEARCHER = new ParallelSearcher(PRD_CONFIG, MoveGenerator::new, MoveOrderer::new, NNUE::new, TRANSPOSITION_TABLE);
    public static final String QUIET_POSITIONS_FILE = "src/test/resources/texel/quiet_positions.epd";

    public static Engine getEngine() {
        return new Engine(PRD_CONFIG, OPENING_BOOK, TABLEBASE, new MoveGenerator(), new Searcher(PRD_CONFIG, new ThreadManager(), new MoveGenerator(), new MoveOrderer(), new NNUE(), new TranspositionTable(PRD_CONFIG.getDefaultHashSizeMb())));
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
        board.setPawns(0L);
        board.setKnights(0L);
        board.setBishops(0L);
        board.setRooks(0L);
        board.setQueens(0L);
        board.setKings(0L);

        board.setWhitePieces(0L);
        board.setBlackPieces(0L);
        board.setOccupied(0L);
        board.setPieceList(new Piece[64]);

        board.getGameState().setCastlingRights(0b0000);

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

    public static List<String> loadFens(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllLines(path);
    }


}
