package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Piece;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class Accumulator {

    public static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    /**
     * Two feature vectors, one from white's perspective, one from black's.
     */
    public short[] whiteFeatures;
    public short[] blackFeatures;
    public AccumulatorUpdate update;
    public boolean dirty = false;

    public Accumulator(int featureCount) {
        this.whiteFeatures = new short[featureCount];
        this.blackFeatures = new short[featureCount];
        this.update = new AccumulatorUpdate();
    }

    public Accumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.update = new AccumulatorUpdate();
    }

    public void add(int wx1, int bx1) {
        add(whiteFeatures, blackFeatures, wx1, bx1);
    }

    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        addSub(whiteFeatures, blackFeatures, wx1, bx1, wx2, bx2);
    }

    public void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        addSubSub(whiteFeatures, blackFeatures, wx1, bx1, wx2, bx2, wx3, bx3);
    }

    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        addAddSubSub(whiteFeatures, blackFeatures, wx1, bx1, wx2, bx2, wx3, bx3, wx4, bx4);
    }

    public void add(short[] white, short[] black, int wx1, int bx1) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(white.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, white, i);
            var blackVector = ShortVector.fromArray(SPECIES, black, i);

            var whiteAddVector = whiteVector.add(ShortVector.fromArray(SPECIES, weights, i + wx1 * hiddenSize));
            var blackAddVector = blackVector.add(ShortVector.fromArray(SPECIES, weights, i + bx1 * hiddenSize));

            whiteAddVector.intoArray(whiteFeatures, i);
            blackAddVector.intoArray(blackFeatures, i);
        }
    }

    public void addSub(short[] white, short[] black, int wx1, int bx1, int wx2, int bx2) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, white, i);
            var blackVector = ShortVector.fromArray(SPECIES, black, i);

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

    public void addSubSub(short[] white, short[] black, int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, white, i);
            var blackVector = ShortVector.fromArray(SPECIES, black, i);

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

    public void addAddSubSub(short[] white, short[] black, int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        short[] weights = NNUE.Network.NETWORK.inputWeights();
        int hiddenSize = NNUE.Network.HIDDEN_SIZE;

        for (int i = 0; i < SPECIES.loopBound(whiteFeatures.length); i += SPECIES.length()) {
            var whiteVector = ShortVector.fromArray(SPECIES, white, i);
            var blackVector = ShortVector.fromArray(SPECIES, black, i);

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

    public record FeatureUpdate(int square, Piece piece, boolean white) {}

    /**
     * A collection of {@link FeatureUpdate} updates representing a single move. This is used to update the accumulator
     * with the changes in features that result from a move.
     */
    public static class AccumulatorUpdate {

        public FeatureUpdate[] adds = new FeatureUpdate[2];
        public int addCount = 0;

        public FeatureUpdate[] subs = new FeatureUpdate[2];
        public int subCount = 0;

        public void pushAdd(FeatureUpdate update) {
            adds[addCount++] = update;
        }

        public void pushSub(FeatureUpdate update) {
            subs[subCount++] = update;
        }

        public void pushAddSub(FeatureUpdate add, FeatureUpdate sub) {
            pushAdd(add);
            pushSub(sub);
        }

        public void pushAddSubSub(FeatureUpdate add, FeatureUpdate sub1, FeatureUpdate sub2) {
            pushAdd(add);
            pushSub(sub1);
            pushSub(sub2);
        }

        public void pushAddAddSubSub(FeatureUpdate add1, FeatureUpdate add2, FeatureUpdate sub1, FeatureUpdate sub2) {
            pushAdd(add1);
            pushAdd(add2);
            pushSub(sub1);
            pushSub(sub2);
        }

    }

}