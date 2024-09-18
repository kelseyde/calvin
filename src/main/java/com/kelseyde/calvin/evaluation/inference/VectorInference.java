package com.kelseyde.calvin.evaluation.inference;

import com.kelseyde.calvin.evaluation.NNUE.Network;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class VectorInference implements Inference {

    static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    static final int UPPER_BOUND = SPECIES.loopBound(Network.HIDDEN_SIZE);

    static final ShortVector FLOOR = ShortVector.broadcast(SPECIES, 0);
    static final ShortVector CEIL = ShortVector.broadcast(SPECIES, Network.QA);

    public VectorInference() {}

    /**
     * Forward pass through the network, using the clipped ReLU activation function.
     * Implementation uses the Java Vector API to perform SIMD operations on multiple features at once.
     */
    public int forward(short[] features, int offset) {
        short[] weights = Network.NETWORK.outputWeights();
        int sum = 0;

        for (int i = 0; i < UPPER_BOUND; i += SPECIES.length()) {
            sum += ShortVector.fromArray(SPECIES, features, i)
                    .min(CEIL)
                    .max(FLOOR)
                    .mul(ShortVector.fromArray(SPECIES, weights, i + offset))
                    .reduceLanes(VectorOperators.ADD);
        }

        return sum;
    }

}
