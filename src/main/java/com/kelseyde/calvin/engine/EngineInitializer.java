package com.kelseyde.calvin.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.endgame.LichessTablebase;
import com.kelseyde.calvin.endgame.Tablebase;
import com.kelseyde.calvin.evaluation.Evaluation;
import com.kelseyde.calvin.evaluation.NNUE;
import com.kelseyde.calvin.generation.MoveGeneration;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.opening.OpeningBook;
import com.kelseyde.calvin.search.ParallelSearcher;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EngineInitializer {

    static final String DEFAULT_CONFIG_LOCATION = "/engine_config.json";
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Engine loadEngine() {
        EngineConfig config = loadDefaultConfig();
        config.postInitialise();
        OpeningBook book = loadDefaultOpeningBook(config);
        Tablebase tablebase = loadDefaultTablebase(config);
        TranspositionTable transpositionTable = new TranspositionTable(config.getDefaultHashSizeMb());
        Supplier<MoveGeneration> moveGenerator = MoveGenerator::new;
        Supplier<MoveOrdering> moveOrderer = MoveOrderer::new;
        Supplier<Evaluation> evaluator = NNUE::new;
        Search searcher = new ParallelSearcher(config, moveGenerator, moveOrderer, evaluator, transpositionTable);
        Engine engine = new Engine(config, book, tablebase, moveGenerator.get(), searcher);
        engine.setPosition(new Board());
        return engine;
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

    public static OpeningBook loadDefaultOpeningBook(EngineConfig config) {
        return loadOpeningBook(config.getOwnBookFile());
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

    public static NNUE.Network loadNetwork(String file, int inputSize, int hiddenSize) {
        try {
            InputStream inputStream = NNUE.Network.class.getClassLoader().getResourceAsStream(file);
            if (inputStream == null) {
                throw new FileNotFoundException("NNUE file not found in resources");
            }

            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();
            ByteBuffer buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN);

            int inputWeightsOffset = inputSize * hiddenSize;
            int inputBiasesOffset = hiddenSize;
            int outputWeightsOffset = hiddenSize * 2;

            short[] inputWeights = new short[inputWeightsOffset];
            short[] inputBiases = new short[inputBiasesOffset];
            short[] outputWeights = new short[outputWeightsOffset];

            for (int i = 0; i < inputWeightsOffset; i++) {
                inputWeights[i] = buffer.getShort();
            }

            for (int i = 0; i < inputBiasesOffset; i++) {
                inputBiases[i] = buffer.getShort();
            }

            for (int i = 0; i < outputWeightsOffset; i++) {
                outputWeights[i] = buffer.getShort();
            }

            short outputBias = buffer.getShort();

            while (buffer.hasRemaining()) {
                if (buffer.getShort() != 0) {
                    throw new RuntimeException("Failed to load NNUE network: invalid file format");
                }
            }

            return new NNUE.Network(inputWeights, inputBiases, outputWeights, outputBias);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load NNUE network", e);
        }
    }

}
