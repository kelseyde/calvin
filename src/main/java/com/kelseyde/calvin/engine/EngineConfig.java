package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.uci.UCI;
import com.kelseyde.calvin.uci.UCICommand;

import java.util.Optional;
import java.util.Set;

public class EngineConfig {

    public EngineConfig() {
        postInitialise();
    }

    public final int minThreads = 1;
    public final int maxThreads = 12;
    public final int defaultThreads = 1;

    public final int minHashSizeMb = 16;
    public final int maxHashSizeMb = 1024;
    public final int defaultHashSizeMb = 256;

    public boolean ponderEnabled = false;
    public boolean pondering = false;
    public boolean searchCancelled = false;

    public final Tunable aspMargin =        new Tunable("AspMargin", 25, 0, 250, 25);
    public final Tunable aspFailMargin =    new Tunable("AspFailMargin", 150, 0, 300, 25);
    public final Tunable aspMaxReduction =  new Tunable("AspMaxReduction", 3, 0, 5, 1);
    public final Tunable nmpDepth =         new Tunable("NmpDepth", 0, 0, 6, 1);
    public final Tunable fpDepth =          new Tunable("FpDepth", 6, 0, 8, 1);
    public final Tunable rfpDepth =         new Tunable("RfpDepth", 5, 0, 8, 1);
    public final Tunable lmrDepth =         new Tunable("LmrDepth", 2, 0, 8, 1);
    public final Tunable lmrBase =          new Tunable("LmrBase", 85, 50, 100, 5);
    public final Tunable lmrDivisor =       new Tunable("LmrDivisor", 310, 200, 400, 10);
    public final Tunable lmrMinMoves =      new Tunable("LmrMinSearchedMoves", 3, 2, 5, 1);
    public final Tunable lmpBaseMoves =     new Tunable("LmpBaseMoves", 3, 2, 5, 1);
    public final Tunable iirDepth =         new Tunable("IirDepth", 4, 0, 8, 1);
    public final Tunable nmpMargin =        new Tunable("NmpMargin", 70, 0, 250, 10);
    public final Tunable dpMargin =         new Tunable("DpMargin", 140, 0, 250, 10);
    public final Tunable qsFpMargin =       new Tunable("QsFpMargin", 100, 0, 250, 10);
    public final Tunable fpMargin =         new Tunable("FpMargin", 275, 0, 500, 10);
    public final Tunable fpScale =          new Tunable("FpScale", 65, 0, 100, 5);
    public final Tunable rfpMargin =        new Tunable("RfpMargin", 75, 0, 250, 10);
    public final Tunable rfpImpMargin =     new Tunable("RfpImpMargin", 40, 0, 250, 10);
    public int[][] lmrReductions;
    public int[][] lmpMoves;

    public Set<Tunable> getTunables() {
        return Set.of(aspMargin, aspFailMargin, aspMaxReduction, nmpDepth, fpDepth, rfpDepth,
                lmrDepth, lmrBase, lmrDivisor, lmrMinMoves, lmpBaseMoves, iirDepth, nmpMargin,
                dpMargin, qsFpMargin, fpMargin, fpScale, rfpMargin, rfpImpMargin);
    }

    public void setTunable(UCICommand command) {
        String name = command.getString("name", "", false);
        if (name.isBlank()) {
            UCI.write("info error missing required option name");
            return;
        }
        int value = command.getInt("value", -1, false);
        if (value == -1) {
            UCI.write("info error missing required option value");
            return;
        }
        Optional<Tunable> tunable = getTunables().stream().filter(t -> t.name.equals(name)).findFirst();
        if (tunable.isEmpty()) {
            UCI.write("info error no option found with name " + name);
            return;
        }
        Tunable option = tunable.get();
        if (value < option.min || value > option.max) {
            UCI.write("info error value " + value + " is out of range for option " + name);
        }
        option.value = value;
        if (name.equals("LmrBase") || name.equals("LmrDivisor")) {
            calculateLmrReductions();
        }

        UCI.write("info string " + name + " " + value);
    }

    public void postInitialise() {
        calculateLmrReductions();
        calculateLmpMoves();
    }

    private void calculateLmrReductions() {
        float lmrBaseFloat = (float) lmrBase.value / 100;
        float lmrDivisorFloat = (float) lmrDivisor.value / 100;
        lmrReductions = new int[Search.MAX_DEPTH][];
        for (int depth = 1; depth < Search.MAX_DEPTH; ++depth) {
            lmrReductions[depth] = new int[250];
            for (int movesSearched = 1; movesSearched < 250; ++movesSearched) {
                lmrReductions[depth][movesSearched] = (int) Math.round(lmrBaseFloat + (Math.log(movesSearched) * Math.log(depth) / lmrDivisorFloat));
            }
        }
    }

    private void calculateLmpMoves() {
        lmpMoves = new int[2][16];
        int base = lmpBaseMoves.value;
        for (int improving = 0; improving < 2; improving++) {
            for (int depth = 0; depth < 16; depth++) {
                lmpMoves[improving][depth] = (base + depth * depth) / (2 - improving);
            }
        }
    }

    public static class Tunable {
        public final String name;
        public int value;
        public final int min;
        public final int max;
        public final int step;

        public Tunable(String name, int value, int min, int max, int step) {
            this.name = name;
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public String toUCI() {
            return String.format("option name %s type spin default %d min %d max %d", name, value, min, max);
        }

    }

}
