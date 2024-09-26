package com.kelseyde.calvin.evaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Network {

    public final InputLayer inputLayer;
    public final int hiddenSize;
    public final int[] quantization;
    public final int scale;

    public short[] inputWeights;
    public short[] inputBiases;
    public short[] outputWeights;
    public short outputBias;

    public Network(String file, InputLayer inputLayer, int hiddenSize, int[] quantization, int scale) {
        this.inputLayer = inputLayer;
        this.hiddenSize = hiddenSize;
        this.quantization = quantization;
        this.scale = scale;
        loadWeights(file, inputLayer, hiddenSize);
    }

    public enum InputLayer {
        CHESS_768(768);

        public final int size;

        InputLayer(int size) {
            this.size = size;
        }
    }

    private void loadWeights(String file, InputLayer inputLayer, int hiddenSize) {
        try {
            InputStream inputStream = Network.class.getClassLoader().getResourceAsStream(file);
            if (inputStream == null) {
                throw new FileNotFoundException("NNUE file not found in resources");
            }

            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();
            ByteBuffer buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN);

            int inputWeightsOffset = inputLayer.size * hiddenSize;
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

            this.inputWeights = inputWeights;
            this.inputBiases = inputBiases;
            this.outputWeights = outputWeights;
            this.outputBias = outputBias;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load NNUE network", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String file;
        private InputLayer inputLayer;
        private int hiddenSize;
        private int[] quantization;
        private int scale;

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder inputSize(InputLayer inputLayer) {
            this.inputLayer = inputLayer;
            return this;
        }

        public Builder hiddenSize(int hiddenSize) {
            this.hiddenSize = hiddenSize;
            return this;
        }

        public Builder quantization(int[] quantization) {
            this.quantization = quantization;
            return this;
        }

        public Builder scale(int scale) {
            this.scale = scale;
            return this;
        }

        public Network build() {
            return new Network(file, inputLayer, hiddenSize, quantization, scale);
        }
    }

}
