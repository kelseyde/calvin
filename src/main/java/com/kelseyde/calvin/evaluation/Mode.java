package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.evaluation.accumulator.Accumulator;
import com.kelseyde.calvin.evaluation.accumulator.ScalarAccumulator;
import com.kelseyde.calvin.evaluation.accumulator.VectorAccumulator;
import com.kelseyde.calvin.evaluation.inference.Inference;
import com.kelseyde.calvin.evaluation.inference.ScalarInference;
import com.kelseyde.calvin.evaluation.inference.VectorInference;

import java.util.function.Supplier;

public enum Mode {

    VECTOR(VectorInference::new, VectorAccumulator::new),
    SCALAR(ScalarInference::new, ScalarAccumulator::new);

    Supplier<Inference> inference;
    Supplier<Accumulator> accumulator;

    Mode(Supplier<Inference> inference, Supplier<com.kelseyde.calvin.evaluation.accumulator.Accumulator> accumulator) {
        this.inference = inference;
        this.accumulator = accumulator;
    }

}
