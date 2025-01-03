package com.kelseyde.calvin.evaluation.activation;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import static com.kelseyde.calvin.evaluation.NNUE.NETWORK;
import static jdk.incubator.vector.VectorOperators.S2I;

/**
 * Implementation of NNUE inference using the SCReLU (Squared Clipped Rectified Linear Unit) activation function.
 * SCReLU is defined as follows:
 * <p>
 *     SCReLU(x) = clamp(x, 0, 1)^2
 * </p>
 */
public class Screlu {

    static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    static final int UPPER_BOUND = SPECIES.loopBound(NETWORK.hiddenSize());
    static final int LOOP_LENGTH = SPECIES.length();

    static final ShortVector FLOOR = ShortVector.broadcast(SPECIES, 0);
    static final ShortVector CEIL = ShortVector.broadcast(SPECIES, NETWORK.quantisations()[0]);

    static final int QA = NETWORK.quantisations()[0];
    static final int QB = NETWORK.quantisations()[1];
    static final int QAB = QA * QB;

    public static int forward(short[] us, short[] them) {

        final int scale = NETWORK.scale();
        final short[] weights = NETWORK.outputWeights();

        int eval = 0;

        // Forward-pass through the network, using the squared clipped ReLU activation function.
        // Implementation uses the Java Vector API to perform SIMD operations on multiple features at once.
        for (int i = 0; i < UPPER_BOUND; i += LOOP_LENGTH) {

            ShortVector usInputs     = ShortVector.fromArray(SPECIES, us, i);
            ShortVector themInputs   = ShortVector.fromArray(SPECIES, them, i);
            ShortVector usWeights    = ShortVector.fromArray(SPECIES, weights, i);
            ShortVector themWeights  = ShortVector.fromArray(SPECIES, weights, i + NETWORK.hiddenSize());

            // Clip the inputs to the range [0, 255].
            usInputs = usInputs.max(FLOOR).min(CEIL);
            themInputs = themInputs.max(FLOOR).min(CEIL);

            // Multiply the inputs by the weights.
            final ShortVector usTerms = usInputs.mul(usWeights);
            final ShortVector themTerms = themInputs.mul(themWeights);

            // Split the inputs and weighted terms into low and high parts, to enable 32-bit multiplication.
            final Vector<Integer> usInputsLo     = usInputs.convert(S2I, 0);
            final Vector<Integer> usInputsHi     = usInputs.convert(S2I, 1);
            final Vector<Integer> themInputsLo   = themInputs.convert(S2I, 0);
            final Vector<Integer> themInputsHi   = themInputs.convert(S2I, 1);

            final Vector<Integer> usTermsLo      = usTerms.convert(S2I, 0);
            final Vector<Integer> usTermsHi      = usTerms.convert(S2I, 1);
            final Vector<Integer> themTermsLo    = themTerms.convert(S2I, 0);
            final Vector<Integer> themTermsHi    = themTerms.convert(S2I, 1);

            // Multiply the inputs by the weighted terms, and add the results to the running sum.
            eval += (int) usInputsLo.mul(usTermsLo)
                    .add(usInputsHi.mul(usTermsHi))
                    .add(themInputsLo.mul(themTermsLo))
                    .add(themInputsHi.mul(themTermsHi))
                    .reduceLanesToLong(VectorOperators.ADD);

        }

        // Since squaring the inputs also squares quantisation, we need to divide that out.
        eval /= QA;

        // Add the output bias, scale the result to centipawn space, and divide by the quantisation factor.
        eval += NETWORK.outputBias();
        eval *= scale;
        eval /= QAB;

        return eval;
    }

}
