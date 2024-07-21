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
        short[] weights = NNUE.Network.NETWORK.inputWeights();

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddVector = whiteVector.add(ShortVector.fromArray(SPECIES, weights, i + wx1 * NNUE.Network.HIDDEN_SIZE));
            var blackAddVector = blackVector.add(ShortVector.fromArray(SPECIES, weights, i + bx1 * NNUE.Network.HIDDEN_SIZE));

            whiteAddVector.intoArray(whiteFeatures, i);
            blackAddVector.intoArray(blackFeatures, i);
        }
    }

    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var whiteAddSubVector = whiteVector
                    .add(ShortVector.fromArray(SPECIES, weights, i + wx1 * NNUE.Network.HIDDEN_SIZE))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + wx2 * NNUE.Network.HIDDEN_SIZE));

            var blackAddSubVector = blackVector
                    .add(ShortVector.fromArray(SPECIES, weights, i + bx1 * NNUE.Network.HIDDEN_SIZE))
                    .sub(ShortVector.fromArray(SPECIES, weights, i + bx2 * NNUE.Network.HIDDEN_SIZE));

            whiteAddSubVector.intoArray(whiteFeatures, i);
            blackAddSubVector.intoArray(blackFeatures, i);
        }
    }

    public void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var wx1Vector = ShortVector.fromArray(SPECIES, weights, i + wx1 * NNUE.Network.HIDDEN_SIZE);
            var wx2Vector = ShortVector.fromArray(SPECIES, weights, i + wx2 * NNUE.Network.HIDDEN_SIZE);
            var wx3Vector = ShortVector.fromArray(SPECIES, weights, i + wx3 * NNUE.Network.HIDDEN_SIZE);

            var bx1Vector = ShortVector.fromArray(SPECIES, weights, i + bx1 * NNUE.Network.HIDDEN_SIZE);
            var bx2Vector = ShortVector.fromArray(SPECIES, weights, i + bx2 * NNUE.Network.HIDDEN_SIZE);
            var bx3Vector = ShortVector.fromArray(SPECIES, weights, i + bx3 * NNUE.Network.HIDDEN_SIZE);

            var whiteResultVector = whiteVector.add(wx1Vector).sub(wx2Vector).sub(wx3Vector);
            var blackResultVector = blackVector.add(bx1Vector).sub(bx2Vector).sub(bx3Vector);

            whiteResultVector.intoArray(whiteFeatures, i);
            blackResultVector.intoArray(blackFeatures, i);
        }
    }

    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, whiteFeatures, i);
            var blackVector = ShortVector.fromArray(SPECIES, blackFeatures, i);

            var wx1Vector = ShortVector.fromArray(SPECIES, weights, i + wx1 * NNUE.Network.HIDDEN_SIZE);
            var wx2Vector = ShortVector.fromArray(SPECIES, weights, i + wx2 * NNUE.Network.HIDDEN_SIZE);
            var wx3Vector = ShortVector.fromArray(SPECIES, weights, i + wx3 * NNUE.Network.HIDDEN_SIZE);
            var wx4Vector = ShortVector.fromArray(SPECIES, weights, i + wx4 * NNUE.Network.HIDDEN_SIZE);

            var bx1Vector = ShortVector.fromArray(SPECIES, weights, i + bx1 * NNUE.Network.HIDDEN_SIZE);
            var bx2Vector = ShortVector.fromArray(SPECIES, weights, i + bx2 * NNUE.Network.HIDDEN_SIZE);
            var bx3Vector = ShortVector.fromArray(SPECIES, weights, i + bx3 * NNUE.Network.HIDDEN_SIZE);
            var bx4Vector = ShortVector.fromArray(SPECIES, weights, i + bx4 * NNUE.Network.HIDDEN_SIZE);

            var whiteResultVector = whiteVector.add(wx1Vector).add(wx2Vector).sub(wx3Vector).sub(wx4Vector);
            var blackResultVector = blackVector.add(bx1Vector).add(bx2Vector).sub(bx3Vector).sub(bx4Vector);

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