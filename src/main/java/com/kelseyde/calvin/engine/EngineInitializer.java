package com.kelseyde.calvin.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.endgame.lichess.LichessTablebase;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.nnue.NNUE;
import com.kelseyde.calvin.evaluation.nnue.Network;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.ParallelSearcher;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.transposition.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EngineInitializer {

    static final String DEFAULT_CONFIG_LOCATION = "/engine_config.json";
    static final String DEFAULT_BOOK_LOCATION = "/opening_book.txt";
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Engine loadEngine() {
        EngineConfig config = loadDefaultConfig();
        config.postInitialise();
        OpeningBook book = loadDefaultOpeningBook();
        Tablebase tablebase = loadDefaultTablebase(config);
        TranspositionTable transpositionTable = new TranspositionTable(config.getDefaultHashSizeMb());
        Supplier<MoveGeneration> moveGenerator = MoveGenerator::new;
        Supplier<MoveOrdering> moveOrderer = MoveOrderer::new;
        Supplier<Evaluation> evaluator = NNUE::new;
        Search searcher = new ParallelSearcher(config, moveGenerator, moveOrderer, evaluator, transpositionTable);
        Network.DEFAULT = Network.loadNetwork();
        return new Engine(config, book, tablebase, moveGenerator.get(), searcher);
    }

    public static EngineConfig loadDefaultConfig() {
        return loadConfig(DEFAULT_CONFIG_LOCATION);
    }

    public static EngineConfig loadConfig(String configLocation) {
        try (InputStream inputStream = EngineInitializer.class.getResourceAsStream(configLocation)) {
            EngineConfig config = OBJECT_MAPPER.readValue(inputStream, EngineConfig.class);
            config.postInitialise();
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OpeningBook loadDefaultOpeningBook() {
        return loadOpeningBook(DEFAULT_BOOK_LOCATION);
    }

    public static Tablebase loadDefaultTablebase(EngineConfig config) {
        return new LichessTablebase(config);
    }

    public static OpeningBook loadOpeningBook(String bookLocation) {
        try (InputStream inputStream = EngineInitializer.class.getResourceAsStream(bookLocation)) {
            if (inputStream == null) {
                return null;
            }
            String file = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return new OpeningBook(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
