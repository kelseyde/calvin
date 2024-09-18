package com.kelseyde.calvin.evaluation.inference;

import com.kelseyde.calvin.evaluation.NNUE.Network;

public class ScalarInference implements Inference {

    public ScalarInference() {}

    public int forward(short[] features, int offset) {
        short[] weights = Network.NETWORK.outputWeights();
        int sum = 0;

        for (int i = 0; i < Network.HIDDEN_SIZE; i++) {
            short feature = features[i];
            feature = (short) Math.max(0, Math.min(Network.QA, feature));
            sum += feature * weights[i + offset];
        }

        return sum;
    }

}
