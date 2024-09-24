package com.kelseyde.calvin.evaluation;

import java.util.Arrays;

public class Accumulator {

    private static final int HIDDEN_SIZE = NNUE.Network.HIDDEN_SIZE;
    private static final short[] WEIGHTS = NNUE.Network.NETWORK.inputWeights();

    /**
     * Two feature vectors, one from white's perspective, one from black's.
     */
    public final short[] whiteFeatures;
    public final short[] blackFeatures;
    private final int featureCount;

    public Accumulator(int featureCount) {
        this.whiteFeatures = new short[featureCount];
        this.blackFeatures = new short[featureCount];
        this.featureCount = featureCount;
    }

    public Accumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.featureCount = whiteFeatures.length;
    }

    public void add(int wx1, int bx1) {
        int wOffset = wx1 * HIDDEN_SIZE;
        int bOffset = bx1 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += WEIGHTS[i + wOffset];
            blackFeatures[i] += WEIGHTS[i + bOffset];
        }
    }

    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        int wOffset1 = wx1 * HIDDEN_SIZE;
        int bOffset1 = bx1 * HIDDEN_SIZE;
        int wOffset2 = wx2 * HIDDEN_SIZE;
        int bOffset2 = bx2 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] - WEIGHTS[i + wOffset2]);
            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] - WEIGHTS[i + bOffset2]);
        }
    }

    public void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3) {
        int wOffset1 = wx1 * HIDDEN_SIZE;
        int bOffset1 = bx1 * HIDDEN_SIZE;
        int wOffset2 = wx2 * HIDDEN_SIZE;
        int bOffset2 = bx2 * HIDDEN_SIZE;
        int wOffset3 = wx3 * HIDDEN_SIZE;
        int bOffset3 = bx3 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] - WEIGHTS[i + wOffset2] - WEIGHTS[i + wOffset3]);
            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] - WEIGHTS[i + bOffset2] - WEIGHTS[i + bOffset3]);
        }
    }

    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        int wOffset1 = wx1 * HIDDEN_SIZE;
        int bOffset1 = bx1 * HIDDEN_SIZE;
        int wOffset2 = wx2 * HIDDEN_SIZE;
        int bOffset2 = bx2 * HIDDEN_SIZE;
        int wOffset3 = wx3 * HIDDEN_SIZE;
        int bOffset3 = bx3 * HIDDEN_SIZE;
        int wOffset4 = wx4 * HIDDEN_SIZE;
        int bOffset4 = bx4 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] + WEIGHTS[i + wOffset2] - WEIGHTS[i + wOffset3] - WEIGHTS[i + wOffset4]);
            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] + WEIGHTS[i + bOffset2] - WEIGHTS[i + bOffset3] - WEIGHTS[i + bOffset4]);
        }
    }

    public Accumulator copy() {
        return new Accumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

}