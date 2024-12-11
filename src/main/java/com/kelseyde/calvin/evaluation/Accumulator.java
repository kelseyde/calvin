package com.kelseyde.calvin.evaluation;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class Accumulator {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int HIDDEN_SIZE = NNUE.NETWORK.hiddenSize();
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

    public void add(short[] weights, int index, boolean whitePerspective) {
        final int offset = index * HIDDEN_SIZE;
        final short[] features = whitePerspective ? whiteFeatures : blackFeatures;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, weights, i + offset))
                    .intoArray(features, i);

        }
    }

    public void add(short[] whiteWeights, short[] blackWeights, int wx1, int bx1) {
        final int wOffset = wx1 * HIDDEN_SIZE;
        final int bOffset = bx1 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset))
                    .intoArray(blackFeatures, i);

        }
    }

    public void addSub(short[] whiteWeights, short[] blackWeights, int wx1, int bx1, int wx2, int bx2) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset2))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset2))
                    .intoArray(blackFeatures, i);

        }
    }

    public void addSubSub(short[] whiteWeights, short[] blackWeights, int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset3))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset3))
                    .intoArray(blackFeatures, i);

        }
    }

    public void addAddSubSub(short[] whiteWeights, short[] blackWeights, int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;
        final int wOffset4 = wx4 * HIDDEN_SIZE;
        final int bOffset4 = bx4 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset1))
                    .add(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset3))
                    .sub(ShortVector.fromArray(SPECIES, whiteWeights, i + wOffset4))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset1))
                    .add(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset3))
                    .sub(ShortVector.fromArray(SPECIES, blackWeights, i + bOffset4))
                    .intoArray(blackFeatures, i);

        }
    }

    public Accumulator copy() {
        return new Accumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

}