package com.kelseyde.calvin.evaluation;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class Accumulator {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int HIDDEN_SIZE = NNUE.NETWORK.hiddenSize();
    private static final short[] WEIGHTS = NNUE.NETWORK.inputWeights();
    private static final short[] BIASES = NNUE.NETWORK.inputBiases();

    /**
     * Two feature vectors, one from white's perspective, one from black's.
     */
    public final short[] whiteFeatures;
    public final short[] blackFeatures;
    private final int featureCount;
    private final int loopLength;

    public Accumulator(int featureCount) {
        this.whiteFeatures = new short[featureCount];
        this.blackFeatures = new short[featureCount];
        this.featureCount = featureCount;
        this.loopLength = SPECIES.loopBound(featureCount);
    }

    public Accumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.featureCount = whiteFeatures.length;
        this.loopLength = SPECIES.loopBound(this.featureCount);
    }

    public void reset(boolean whitePerspective) {
        short[] features = whitePerspective ? whiteFeatures : blackFeatures;

        for (int i = 0; i < SPECIES.loopBound(HIDDEN_SIZE); i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, BIASES, i).intoArray(features, i);
        }
    }

    public void add(int index, boolean whitePerspective) {
        final int offset = index * HIDDEN_SIZE;
        final short[] features = whitePerspective ? whiteFeatures : blackFeatures;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset))
                    .intoArray(features, i);

        }
    }

    public Accumulator add(int wx1, int bx1) {
        final int wOffset = wx1 * HIDDEN_SIZE;
        final int bOffset = bx1 * HIDDEN_SIZE;

        short[] newWhiteFeatures = new short[featureCount];
        short[] newBlackFeatures = new short[featureCount];

        for (int i = 0; i < loopLength; i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, this.whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset))
                    .intoArray(newWhiteFeatures, i);

            ShortVector.fromArray(SPECIES, this.blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset))
                    .intoArray(newBlackFeatures, i);
        }

        return new Accumulator(newWhiteFeatures, newBlackFeatures);
    }

    public Accumulator addSub(int wx1, int bx1, int wx2, int bx2) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;

        short[] newWhiteFeatures = new short[featureCount];
        short[] newBlackFeatures = new short[featureCount];

        for (int i = 0; i < loopLength; i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, this.whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset2))
                    .intoArray(newWhiteFeatures, i);

            ShortVector.fromArray(SPECIES, this.blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset2))
                    .intoArray(newBlackFeatures, i);
        }

        return new Accumulator(newWhiteFeatures, newBlackFeatures);
    }

    public Accumulator addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;

        short[] newWhiteFeatures = new short[featureCount];
        short[] newBlackFeatures = new short[featureCount];

        for (int i = 0; i < loopLength; i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, this.whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset3))
                    .intoArray(newWhiteFeatures, i);

            ShortVector.fromArray(SPECIES, this.blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset3))
                    .intoArray(newBlackFeatures, i);
        }

        return new Accumulator(newWhiteFeatures, newBlackFeatures);
    }

    public Accumulator addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;
        final int wOffset4 = wx4 * HIDDEN_SIZE;
        final int bOffset4 = bx4 * HIDDEN_SIZE;

        short[] newWhiteFeatures = new short[featureCount];
        short[] newBlackFeatures = new short[featureCount];

        for (int i = 0; i < loopLength; i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, this.whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset1))
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset3))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset4))
                    .intoArray(newWhiteFeatures, i);

            ShortVector.fromArray(SPECIES, this.blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset1))
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset3))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset4))
                    .intoArray(newBlackFeatures, i);
        }

        return new Accumulator(newWhiteFeatures, newBlackFeatures);
    }

    public Accumulator copy() {
        return new Accumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

}