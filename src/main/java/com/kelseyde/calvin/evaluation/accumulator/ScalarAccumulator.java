package com.kelseyde.calvin.evaluation.accumulator;

import com.kelseyde.calvin.evaluation.NNUE.Network;

import java.util.Arrays;

public class ScalarAccumulator extends Accumulator {

    private final int featureCount;

    public ScalarAccumulator() {
        super();
        this.featureCount = Network.HIDDEN_SIZE;
    }

    public ScalarAccumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.featureCount = whiteFeatures.length;
    }

    @Override
    public void add(int wx1, int bx1) {
        final int wOffset = wx1 * HIDDEN_SIZE;
        final int bOffset = bx1 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += WEIGHTS[i + wOffset];
            blackFeatures[i] += WEIGHTS[i + bOffset];
        }
    }

    @Override
    public void addSub(int wx1, int bx1, int wx2, int bx2) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] - WEIGHTS[i + wOffset2]);
            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] - WEIGHTS[i + bOffset2]);
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

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] - WEIGHTS[i + wOffset2] - WEIGHTS[i + wOffset3]);
            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] - WEIGHTS[i + bOffset2] - WEIGHTS[i + bOffset3]);
        }
    }

    @Override
    public void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4) {
        final int wOffset1 = wx1 * HIDDEN_SIZE;
        final int bOffset1 = bx1 * HIDDEN_SIZE;
        final int wOffset2 = wx2 * HIDDEN_SIZE;
        final int bOffset2 = bx2 * HIDDEN_SIZE;
        final int wOffset3 = wx3 * HIDDEN_SIZE;
        final int bOffset3 = bx3 * HIDDEN_SIZE;
        final int wOffset4 = wx4 * HIDDEN_SIZE;
        final int bOffset4 = bx4 * HIDDEN_SIZE;

        for (int i = 0; i < featureCount; i++) {
            whiteFeatures[i] += (short) (WEIGHTS[i + wOffset1] + WEIGHTS[i + wOffset2]
                    - WEIGHTS[i + wOffset3] - WEIGHTS[i + wOffset4]);

            blackFeatures[i] += (short) (WEIGHTS[i + bOffset1] + WEIGHTS[i + bOffset2]
                    - WEIGHTS[i + bOffset3] - WEIGHTS[i + bOffset4]);
        }
    }

    @Override
    public Accumulator copy() {
        return new ScalarAccumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length)
        );
    }
}