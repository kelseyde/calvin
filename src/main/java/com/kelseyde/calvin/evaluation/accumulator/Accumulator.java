package com.kelseyde.calvin.evaluation.accumulator;

import com.kelseyde.calvin.evaluation.NNUE.Network;

public abstract class Accumulator {

    protected static final int HIDDEN_SIZE = Network.HIDDEN_SIZE;
    protected static final short[] WEIGHTS = Network.NETWORK.inputWeights();

    public short[] whiteFeatures;
    public short[] blackFeatures;

    public Accumulator() {
        this.whiteFeatures = new short[Network.HIDDEN_SIZE];
        this.blackFeatures = new short[Network.HIDDEN_SIZE];
    }

    public abstract void add(int wx1, int bx1);

    public abstract void addSub(int wx1, int bx1, int wx2, int bx2);

    public abstract void addSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3);

    public abstract void addAddSubSub(int wx1, int bx1, int wx2, int bx2, int wx3, int bx3, int wx4, int bx4);

    public abstract Accumulator copy();

}
