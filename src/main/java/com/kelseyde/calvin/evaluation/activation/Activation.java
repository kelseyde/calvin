package com.kelseyde.calvin.evaluation.activation;

public enum Activation {

    SCRELU(Screlu::forward),
    CRELU(Crelu::forward);

    private final ActivationFunction function;

    Activation(ActivationFunction function) {
        this.function = function;
    }

    public int forward(short[] us, short[] them) {
        return function.forward(us, them);
    }

    public interface ActivationFunction {
        int forward(short[] us, short[] them);
    }

}
