package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Colour;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

public class Accumulator {

    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int HIDDEN_SIZE = NNUE.NETWORK.hiddenSize();
    private static final short[] WEIGHTS = NNUE.NETWORK.inputWeights();
    private static final short[] BIASES = NNUE.NETWORK.inputBiases();

    /**
     * Two feature vectors, one from white's perspective, one from black's.
     */
    public final short[] whiteFeatures;
    public final short[] blackFeatures;

    public AccumulatorUpdate update;
    public final boolean[] needsRefresh;
    public final boolean[] computed;
    public final boolean[] mirror;

    private final int featureCount;
    private final int loopLength;

    public Accumulator(int featureCount, boolean whiteMirror, boolean blackMirror) {
        this.whiteFeatures = new short[featureCount];
        this.blackFeatures = new short[featureCount];
        this.needsRefresh = new boolean[2];
        this.computed = new boolean[2];
        this.mirror = new boolean[] {whiteMirror, blackMirror};
        this.featureCount = featureCount;
        this.loopLength = SPECIES.loopBound(featureCount);
    }

    public Accumulator(short[] whiteFeatures, short[] blackFeatures) {
        this.whiteFeatures = whiteFeatures;
        this.blackFeatures = blackFeatures;
        this.needsRefresh = new boolean[2];
        this.computed = new boolean[2];
        this.mirror = new boolean[2];
        this.featureCount = whiteFeatures.length;
        this.loopLength = SPECIES.loopBound(this.featureCount);
    }

    public void reset(boolean whitePerspective) {
        short[] features = whitePerspective ? whiteFeatures : blackFeatures;

        for (int i = 0; i < SPECIES.loopBound(HIDDEN_SIZE); i += SPECIES.length()) {
            ShortVector.fromArray(SPECIES, BIASES, i).intoArray(features, i);
        }
    }

    public void add(Feature feature, boolean whitePerspective, boolean mirror) {
        final int offset = feature.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final short[] features = whitePerspective ? whiteFeatures : blackFeatures;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset))
                    .intoArray(features, i);

        }
    }

    public void apply(Accumulator prev, AccumulatorUpdate update, boolean whitePerspective) {
        final boolean mirror = this.mirror[Colour.index(whitePerspective)];
        switch (update.getUpdateType()) {
            case ADD -> add(prev, update, whitePerspective, mirror);
            case ADD_SUB -> addSub(prev, update, whitePerspective, mirror);
            case ADD_SUB_SUB -> addSubSub(prev, update, whitePerspective, mirror);
            case ADD_ADD_SUB_SUB -> addAddSubSub(prev, update, whitePerspective, mirror);
        }
    }

    public void add(Accumulator prev, AccumulatorUpdate update, boolean whitePerspective, boolean mirror) {

        final short[] features = whitePerspective ? prev.whiteFeatures : prev.blackFeatures;

        final Feature add1 = update.adds[0];

        final int offset = add1.index(whitePerspective, mirror) * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset))
                    .intoArray(features, i);

        }
    }

    public void addSub(Accumulator prev, AccumulatorUpdate update, boolean whitePerspective, boolean mirror) {

        final short[] features = whitePerspective ? prev.whiteFeatures : prev.blackFeatures;

        final Feature add1 = update.adds[0];
        final Feature sub1 = update.subs[0];

        final int offset1 = add1.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset2 = sub1.index(whitePerspective, mirror) * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset2))
                    .intoArray(features, i);

        }
    }

    public void addSubSub(Accumulator prev, AccumulatorUpdate update, boolean whitePerspective, boolean mirror) {

        final short[] features = whitePerspective ? prev.whiteFeatures : prev.blackFeatures;

        final Feature add1 = update.adds[0];
        final Feature sub1 = update.subs[0];
        final Feature sub2 = update.subs[1];

        final int offset1 = add1.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset2 = sub1.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset3 = sub2.index(whitePerspective, mirror) * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset1))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset3))
                    .intoArray(features, i);

        }
    }

    public void addAddSubSub(Accumulator prev, AccumulatorUpdate update, boolean whitePerspective, boolean mirror) {

        final short[] features = whitePerspective ? prev.whiteFeatures : prev.blackFeatures;

        final Feature add1 = update.adds[0];
        final Feature add2 = update.adds[1];
        final Feature sub1 = update.subs[0];
        final Feature sub2 = update.subs[1];

        final int offset1 = add1.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset2 = add2.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset3 = sub1.index(whitePerspective, mirror) * HIDDEN_SIZE;
        final int offset4 = sub2.index(whitePerspective, mirror) * HIDDEN_SIZE;

        for (int i = 0; i < loopLength; i += SPECIES.length()) {

            ShortVector.fromArray(SPECIES, features, i)
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset1))
                    .add(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset2))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset3))
                    .sub(ShortVector.fromArray(SPECIES, WEIGHTS, i + offset4))
                    .intoArray(features, i);

        }
    }

    public Accumulator copy() {
        return new Accumulator(
                Arrays.copyOf(whiteFeatures, whiteFeatures.length),
                Arrays.copyOf(blackFeatures, blackFeatures.length));
    }

    public static class AccumulatorUpdate {

        public AccumulatorUpdate() {}

        public Feature[] adds = new Feature[2];
        public Feature[] subs = new Feature[2];

        public int addCount = 0;
        public int subCount = 0;

        public void pushAdd(Feature add) {
            adds[addCount++] = add;
        }

        public void pushSub(Feature sub) {
            subs[subCount++] = sub;
        }

        public void addSub(Feature add, Feature sub) {
            pushAdd(add);
            pushSub(sub);
        }

        public void addSubSub(Feature add, Feature sub1, Feature sub2) {
            pushAdd(add);
            pushSub(sub1);
            pushSub(sub2);
        }

        public void addAddSubSub(Feature add1, Feature add2, Feature sub1, Feature sub2) {
            pushAdd(add1);
            pushAdd(add2);
            pushSub(sub1);
            pushSub(sub2);
        }

        public UpdateType getUpdateType() {
            if (addCount == 1 && subCount == 0) {
                return UpdateType.ADD;
            }
            else if (addCount == 1 && subCount == 1) {
                return UpdateType.ADD_SUB;
            }
            else if (addCount == 1 && subCount == 2) {
                return UpdateType.ADD_SUB_SUB;
            }
            else if (addCount == 2 && subCount == 2) {
                return UpdateType.ADD_ADD_SUB_SUB;
            }
            else {
                throw new IllegalStateException("Unexpected update type");
            }
        }

    }

    public enum UpdateType {
        ADD,
        ADD_SUB,
        ADD_SUB_SUB,
        ADD_ADD_SUB_SUB
    }

}