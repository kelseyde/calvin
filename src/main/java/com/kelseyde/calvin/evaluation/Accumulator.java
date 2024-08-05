package com.kelseyde.calvin.evaluation;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class Accumulator {

    public static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    /**
     * Two feature vectors, one from white's perspective, one from black's.
     */
    public final short[] whiteFeatures;
    public final short[] blackFeatures;

    public Accumulator(int featureCount) {
        this.whiteFeatures = new short[featureCount];
        this.blackFeatures = new short[featureCount];
    }

    public Accumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
    }

    public void add(int wx1, int bx1) {
        System.out.printf("adding (%s %s)\n", wx1, bx1);
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddVector = whiteVector.add(ShortVector.fromArray(SPECIES, weights, i + wx1 * hiddenSize));
            var blackAddVector = blackVector.add(ShortVector.fromArray(SPECIES, weights, i + bx1 * hiddenSize));

            whiteAddVector.intoArray(whiteFeatures, i);
            blackAddVector.intoArray(blackFeatures, i);
        }
    }

    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        System.out.printf("adding (%s %s), subbing (%s %s)\n", wx1, bx1, wx2, bx2);
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddSubVector = whiteVector
                    .add(ShortVector.fromArray(SPECIES, weights, i + wx1 * hiddenSize))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + wx2 * hiddenSize));

            var blackAddSubVector = blackVector
                    .add(ShortVector.fromArray(SPECIES, weights, i + bx1 * hiddenSize))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + bx2 * hiddenSize));

            whiteAddSubVector.intoArray(whiteFeatures, i);
            blackAddSubVector.intoArray(blackFeatures, i);
        }
    }

    public void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        System.out.printf("adding (%s %s), subbing (%s %s), subbing (%s %s)\n", wx1, bx1, wx2, bx2, wx3, bx3);
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddVector1 = ShortVector.fromArray(SPECIES, weights, i + wx1 * hiddenSize);
            var whiteSubVector1 = ShortVector.fromArray(SPECIES, weights, i + wx2 * hiddenSize);
            var whiteSubVector2 = ShortVector.fromArray(SPECIES, weights, i + wx3 * hiddenSize);

            var blackAddVector1 = ShortVector.fromArray(SPECIES, weights, i + bx1 * hiddenSize);
            var blackSubVector1 = ShortVector.fromArray(SPECIES, weights, i + bx2 * hiddenSize);
            var blackSubVector2 = ShortVector.fromArray(SPECIES, weights, i + bx3 * hiddenSize);

            var whiteResultVector = whiteVector.add(whiteAddVector1).sub(whiteSubVector1).sub(whiteSubVector2);
            var blackResultVector = blackVector.add(blackAddVector1).sub(blackSubVector1).sub(blackSubVector2);

            whiteResultVector.intoArray(whiteFeatures, i);
            blackResultVector.intoArray(blackFeatures, i);
        }
    }

    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        System.out.printf("adding (%s %s), subbing (%s %s), subbing (%s %s), subbing (%s %s)\n", wx1, bx1, wx2, bx2, wx3, bx3, wx4, bx4);
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddVector1 = ShortVector.fromArray(SPECIES, weights, i + wx1 * hiddenSize);
            var whiteAddVector2 = ShortVector.fromArray(SPECIES, weights, i + wx2 * hiddenSize);
            var whiteSubVector1 = ShortVector.fromArray(SPECIES, weights, i + wx3 * hiddenSize);
            var whiteSubVector2 = ShortVector.fromArray(SPECIES, weights, i + wx4 * hiddenSize);

            var blackAddVector1 = ShortVector.fromArray(SPECIES, weights, i + bx1 * hiddenSize);
            var blackAddVector2 = ShortVector.fromArray(SPECIES, weights, i + bx2 * hiddenSize);
            var blackSubVector1 = ShortVector.fromArray(SPECIES, weights, i + bx3 * hiddenSize);
            var blackSubVector2 = ShortVector.fromArray(SPECIES, weights, i + bx4 * hiddenSize);

            var whiteResultVector = whiteVector
                    .add(whiteAddVector1).add(whiteAddVector2)
                    .sub(whiteSubVector1).sub(whiteSubVector2);

            var blackResultVector = blackVector
                    .add(blackAddVector1).add(blackAddVector2)
                    .sub(blackSubVector1).sub(blackSubVector2);

            whiteResultVector.intoArray(whiteFeatures, i);
            blackResultVector.intoArray(blackFeatures, i);
        }
    }

    public Accumulator copy() {
        return new Accumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

}