package com.kelseyde.calvin.evaluation.accumulator;


import com.kelseyde.calvin.evaluation.NNUE.Network;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class VectorAccumulator extends Accumulator {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    private final int featureCount;
    private final int loopLength;

    public VectorAccumulator() {
        super();
        this.featureCount = Network.HIDDEN_SIZE;
        this.loopLength = SPECIES.loopBound(featureCount);
    }

    public VectorAccumulator(short[] whiteFeatures, short[] blackFeatures) {
        super();
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.featureCount = whiteFeatures.length;
        this.loopLength = SPECIES.loopBound(this.featureCount);
    }

    @Override
    public void add(int wx1, int bx1) {
        final int wOffset = wx1 * HIDDEN_SIZE;
        final int bOffset = bx1 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset))
                    .intoArray(blackFeatures, i);

        }
    }

    @Override
    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset2))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset2))
                    .intoArray(blackFeatures, i);

        }
    }

    @Override
    public void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, whiteFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + wOffset3))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + bOffset3))
                    .intoArray(blackFeatures, i);

        }
    }

    @Override
    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        short[] weights = Network.NETWORK.inputWeights();
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
                    .add(ShortVector.fromArray(SPECIES, weights, i + wOffset1))
                    .add(ShortVector.fromArray(SPECIES, weights, i + wOffset2))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + wOffset3))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + wOffset4))
                    .intoArray(whiteFeatures, i);

            ShortVector.fromArray(SPECIES, blackFeatures, i)
                    .add(ShortVector.fromArray(SPECIES, weights, i + bOffset1))
                    .add(ShortVector.fromArray(SPECIES, weights, i + bOffset2))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + bOffset3))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + bOffset4))
                    .intoArray(blackFeatures, i);

        }
    }

    @Override
    public Accumulator copy() {
        return new VectorAccumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

}
