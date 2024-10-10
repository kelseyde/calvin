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

    public final Tunable aspMargin =        new Tunable("AspMargin", 20, 0, 250, 10);
    public final Tunable aspFailMargin =    new Tunable("AspFailMargin", 105, 0, 300, 10);
    public final Tunable aspMaxReduction =  new Tunable("AspMaxReduction", 0, 0, 5, 1);
    public final Tunable nmpDepth =         new Tunable("NmpDepth", 0, 0, 6, 1);
    public final Tunable nmpMargin =        new Tunable("NmpMargin", 18, 0, 250, 10);
    public final Tunable nmpImpMargin =     new Tunable("NmpImpMargin", 52, 0, 250, 10);
    public final Tunable nmpBase =          new Tunable("NmpBase", 3, 0, 6, 1);
    public final Tunable nmpDivisor =       new Tunable("NmpDivisor", 2, 1, 4, 1);
    public final Tunable fpDepth =          new Tunable("FpDepth", 5, 0, 8, 1);
    public final Tunable fpMargin =         new Tunable("FpMargin", 259, 0, 500, 10);
    public final Tunable fpImpMargin =      new Tunable("FpImpMargin", 350, 0, 500, 10);
    public final Tunable fpScale =          new Tunable("FpScale", 68, 0, 100, 5);
    public final Tunable qsFpMargin =       new Tunable("QsFpMargin", 99, 0, 250, 10);
    public final Tunable qsSeeEqualDepth =  new Tunable("QsSeeEqualDepth", 3, 0, 6, 1);
    public final Tunable rfpDepth =         new Tunable("RfpDepth", 7, 0, 8, 1);
    public final Tunable rfpMargin =        new Tunable("RfpMargin", 86, 0, 250, 10);
    public final Tunable rfpImpMargin =     new Tunable("RfpImpMargin", 43, 0, 250, 10);
    public final Tunable lmrDepth =         new Tunable("LmrDepth", 2, 0, 8, 1);
    public final Tunable lmrBase =          new Tunable("LmrBase", 90, 50, 100, 5);
    public final Tunable lmrDivisor =       new Tunable("LmrDivisor", 310, 200, 400, 10);
    public final Tunable lmrMinMoves =      new Tunable("LmrMinMoves", 3, 2, 5, 1);
    public final Tunable lmrMinPvMoves =    new Tunable("LmrMinPvMoves", 4, 2, 5, 1);
    public final Tunable lmpDepth =         new Tunable("LmpDepth", 4, 0, 8, 1);
    public final Tunable lmpMultiplier =    new Tunable("LmpMultiplier", 8, 1, 20, 1);
    public final Tunable iirDepth =         new Tunable("IirDepth", 3, 0, 8, 1);
    public final Tunable dpMargin =         new Tunable("DpMargin", 111, 0, 250, 10);
    public final Tunable razorDepth =       new Tunable("RazorDepth", 5, 0, 8, 1);
    public final Tunable razorMargin =      new Tunable("RazorMargin", 496, 0, 600, 10);
    public final Tunable hpMaxDepth =       new Tunable("HpMaxDepth", 3, 0, 10, 1);
    public final Tunable hpMargin =         new Tunable("HpMargin", -2197, -4000, -100, 50);
    public final Tunable hpOffset =         new Tunable("HpOffset", -1039, -3000, 0, 50);
    public final Tunable quietHistBonus =   new Tunable("QuietHistBonus", 1200, 100, 2000, 100);
    public final Tunable quietHistMalus =   new Tunable("QuietHistMalus", 1200, 100, 2000, 100);
    public final Tunable quietHistMax =     new Tunable("QuietHistMax", 8192, 1000, 12000, 100);
    public final Tunable captHistBonus =    new Tunable("CaptHistBonus", 1200, 100, 2000, 100);
    public final Tunable captHistMalus =    new Tunable("CaptHistMalus", 1200, 100, 2000, 100);
    public final Tunable captHistMax =      new Tunable("CaptHistMax", 8192, 1000, 12000, 100);
    public final Tunable contHistBonus =    new Tunable("ContHistBonus", 1200, 100, 2000, 100);
    public final Tunable contHistMalus =    new Tunable("ContHistMalus", 1200, 100, 2000, 100);
    public final Tunable contHistMax =      new Tunable("ContHistMax", 8192, 1000, 12000, 100);
    public final Tunable nodeTmMinDepth =   new Tunable("NodeTmMinDepth", 5, 0, 10, 1);
    public final Tunable nodeTmBase =       new Tunable("NodeTmBase", 150, 100, 200, 10);
    public final Tunable nodeTmScale =      new Tunable("NodeTmScale", 135, 100, 200, 10);

    public int[][] lmrReductions;

    public Set<Tunable> getTunables() {
        return Set.of(
                aspMargin, aspFailMargin, aspMaxReduction, nmpDepth, fpDepth, rfpDepth, lmrDepth, lmrBase,
                lmrDivisor, lmrMinMoves, lmrMinPvMoves, lmpDepth, lmpMultiplier, iirDepth, nmpMargin, nmpImpMargin, nmpBase, nmpDivisor,
                dpMargin, qsFpMargin, qsSeeEqualDepth, fpMargin, fpScale, rfpMargin, rfpImpMargin, razorDepth, razorMargin,
                hpMaxDepth, hpMargin, hpOffset, nodeTmMinDepth, nodeTmBase, nodeTmScale
        );
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
