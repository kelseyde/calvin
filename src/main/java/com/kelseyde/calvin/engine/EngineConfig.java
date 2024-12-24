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

    public final int minHashSizeMb = 8;
    public final int maxHashSizeMb = 1024;
    public final int defaultHashSizeMb = 256;

    public boolean ponderEnabled = false;
    public boolean pondering = false;
    public boolean searchCancelled = false;

    public final Tunable aspMargin              = new Tunable("AspMargin", 20, 0, 250, 25);
    public final Tunable aspFailMargin          = new Tunable("AspFailMargin", 105, 0, 300, 25);
    public final Tunable aspMaxReduction        = new Tunable("AspMaxReduction", 0, 0, 5, 1);
    public final Tunable nmpDepth               = new Tunable("NmpDepth", 0, 0, 6, 1);
    public final Tunable nmpMargin              = new Tunable("NmpMargin", 18, 0, 250, 25);
    public final Tunable nmpImpMargin           = new Tunable("NmpImpMargin", 52, 0, 250, 25);
    public final Tunable nmpBase                = new Tunable("NmpBase", 3, 0, 6, 1);
    public final Tunable nmpDivisor             = new Tunable("NmpDivisor", 2, 1, 4, 1);
    public final Tunable nmpEvalScale           = new Tunable("NmpEvalScale", 210, 0, 400, 25);
    public final Tunable nmpEvalMaxReduction    = new Tunable("NmpEvalMaxReduction", 4, 2, 5, 1);
    public final Tunable fpDepth                = new Tunable("FpDepth", 5, 0, 8, 1);
    public final Tunable fpMargin               = new Tunable("FpMargin", 259, 0, 500, 25);
    public final Tunable fpScale                = new Tunable("FpScale", 68, 0, 100, 5);
    public final Tunable fpBlend                = new Tunable("FpBlend", 4, 1, 10, 2);
    public final Tunable seeMaxDepth            = new Tunable("SeeMaxDepth", 10, 6, 12, 1);
    public final Tunable seeQuietMargin         = new Tunable("SeeQuietMargin", -40, -250, -10, 25);
    public final Tunable seeNoisyMargin         = new Tunable("SeeNoisyMargin", -20, -250, -10, 25);
    public final Tunable seeNoisyOffset         = new Tunable("SeeNoisyOffset", 15, -100, 200, 50);
    public final Tunable qsFpMargin             = new Tunable("QsFpMargin", 99, 0, 250, 10);
    public final Tunable qsSeeEqualDepth        = new Tunable("QsSeeEqualDepth", 3, 0, 6, 1);
    public final Tunable rfpDepth               = new Tunable("RfpDepth", 7, 0, 8, 1);
    public final Tunable rfpMargin              = new Tunable("RfpMargin", 86, 0, 250, 25);
    public final Tunable rfpImpMargin           = new Tunable("RfpImpMargin", 43, 0, 250, 25);
    public final Tunable rfpBlend               = new Tunable("RfpBlend", 4, 1, 10, 2);
    public final Tunable lmrDepth               = new Tunable("LmrDepth", 2, 0, 8, 1);
    public final Tunable lmrBase                = new Tunable("LmrBase", 90, 50, 100, 5);
    public final Tunable lmrDivisor             = new Tunable("LmrDivisor", 310, 200, 400, 10);
    public final Tunable lmrCapBase             = new Tunable("LmrCapBase", 90, 50, 100, 5);
    public final Tunable lmrCapDivisor          = new Tunable("LmrCapDivisor", 310, 200, 400, 10);
    public final Tunable lmrMinMoves            = new Tunable("LmrMinMoves", 3, 2, 5, 1);
    public final Tunable lmrMinPvMoves          = new Tunable("LmrMinPvMoves", 4, 2, 5, 1);
    public final Tunable lmpDepth               = new Tunable("LmpDepth", 4, 0, 8, 1);
    public final Tunable lmpMultiplier          = new Tunable("LmpMultiplier", 8, 1, 20, 1);
    public final Tunable iirDepth               = new Tunable("IirDepth", 3, 0, 8, 1);
    public final Tunable dpMargin               = new Tunable("DpMargin", 111, 0, 250, 10);
    public final Tunable razorDepth             = new Tunable("RazorDepth", 5, 0, 8, 1);
    public final Tunable razorMargin            = new Tunable("RazorMargin", 496, 0, 600, 10);
    public final Tunable hpMaxDepth             = new Tunable("HpMaxDepth", 3, 0, 10, 1);
    public final Tunable hpMargin               = new Tunable("HpMargin", -2197, -4000, -100, 50);
    public final Tunable hpOffset               = new Tunable("HpOffset", -1039, -3000, 0, 50);
    public final Tunable ttExtensionDepth       = new Tunable("TtExtDepth", 6, 0, 12, 1);
    public final Tunable quietHistBonusMax      = new Tunable("QuietHistBonusMax", 1200, 100, 2000, 100);
    public final Tunable quietHistBonusScale    = new Tunable("QuietHistBonusScale", 200, 50, 400, 25);
    public final Tunable quietHistMalusMax      = new Tunable("QuietHistMalusMax", 1200, 100, 2000, 100);
    public final Tunable quietHistMalusScale    = new Tunable("QuietHistMalusScale", 200, 50, 400, 25);
    public final Tunable quietHistMaxScore      = new Tunable("QuietHistMaxScore", 8192, 1000, 12000, 100);
    public final Tunable captHistBonusMax       = new Tunable("CaptHistBonusMax", 1200, 100, 2000, 100);
    public final Tunable captHistBonusScale     = new Tunable("CaptHistBonusScale", 200, 50, 400, 25);
    public final Tunable captHistMalusMax       = new Tunable("CaptHistMalusMax", 1200, 100, 2000, 100);
    public final Tunable captHistMalusScale     = new Tunable("CaptHistMalusScale", 200, 50, 400, 25);
    public final Tunable captHistMaxScore       = new Tunable("CaptHistMaxScore", 8192, 1000, 12000, 100);
    public final Tunable contHistBonusMax       = new Tunable("ContHistBonusMax", 1200, 100, 2000, 100);
    public final Tunable contHistBonusScale     = new Tunable("ContHistBonusScale", 200, 50, 400, 25);
    public final Tunable contHistMalusMax       = new Tunable("ContHistMalusMax", 1200, 100, 2000, 100);
    public final Tunable contHistMalusScale     = new Tunable("ContHistMalusScale", 200, 50, 400, 25);
    public final Tunable contHistMaxScore       = new Tunable("ContHistMaxScore", 8192, 1000, 12000, 100);
    public final Tunable timeFactor             = new Tunable("TimeFactor", 5, 3, 10, 1);
    public final Tunable incrementFactor        = new Tunable("IncrementFactor", 75, 50, 100, 5);
    public final Tunable softTimeFactor         = new Tunable("SoftTimeFactor", 66, 50, 70, 10);
    public final Tunable hardTimeFactor         = new Tunable("HardTimeFactor", 200, 150, 250, 10);
    public final Tunable softTimeScaleMin       = new Tunable("SoftTimeScaleMin", 12, 10, 25, 2);
    public final Tunable softTimeScaleMax       = new Tunable("SoftTimeScaleMax", 250, 100, 250, 50);
    public final Tunable uciOverhead            = new Tunable("UciOverhead", 50, 0, 1000, 50);
    public final Tunable nodeTmMinDepth         = new Tunable("NodeTmMinDepth", 5, 0, 10, 1);
    public final Tunable nodeTmBase             = new Tunable("NodeTmBase", 150, 100, 200, 10);
    public final Tunable nodeTmScale            = new Tunable("NodeTmScale", 135, 100, 200, 10);
    public final Tunable bmStabilityMinDepth    = new Tunable("BmStabilityMinDepth", 0, 0, 10, 1);
    public final Tunable scoreStabilityMinDepth = new Tunable("ScoreStabilityMinDepth", 0, 0, 10, 1);

    public int[][][] lmrReductions;
    public final int[] bmStabilityFactor = { 250, 120, 90, 80, 75 };
    public final int[] scoreStabilityFactor = { 125, 115, 100, 94, 88 };
    public final int[] contHistPlies = { 1, 2 };

    public Set<Tunable> getTunables() {
        return Set.of(
                aspMargin, aspFailMargin, aspMaxReduction, nmpDepth, nmpEvalScale, nmpEvalMaxReduction, fpDepth, fpBlend,
                rfpDepth, lmrDepth, lmrBase, lmrDivisor, lmrCapBase, lmrCapDivisor, lmrMinMoves, lmrMinPvMoves, lmpDepth,
                lmpMultiplier, iirDepth, nmpMargin, nmpImpMargin, nmpBase, nmpDivisor, dpMargin, qsFpMargin,
                qsSeeEqualDepth, fpMargin, fpScale, rfpMargin, rfpImpMargin, rfpBlend, razorDepth, razorMargin,
                hpMaxDepth, hpMargin, hpOffset, quietHistBonusMax, quietHistBonusScale, quietHistMalusMax,
                quietHistMalusScale, quietHistMaxScore, captHistBonusMax, captHistBonusScale, captHistMalusMax,
                captHistMalusScale, captHistMaxScore, contHistBonusMax, contHistBonusScale, contHistMalusMax,
                contHistMalusScale, contHistMaxScore, nodeTmMinDepth, nodeTmBase, nodeTmScale, ttExtensionDepth,
                seeMaxDepth, seeQuietMargin, seeNoisyMargin, seeNoisyOffset, timeFactor, incrementFactor, softTimeFactor,
                hardTimeFactor, softTimeScaleMin, softTimeScaleMax, uciOverhead, bmStabilityMinDepth, scoreStabilityMinDepth
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
        if (name.equals("LmrBase") || name.equals("LmrDivisor")
                || name.equals("LmrCapBase") || name.equals("LmrCapDivisor")) {
            calculateLmrTable();
        }

        UCI.write("info string " + name + " " + value);
    }

    public void postInitialise() {
        calculateLmrTable();
    }

    private void calculateLmrTable() {
        float quietBase = (float) lmrBase.value / 100;
        float quietDivisor = (float) lmrDivisor.value / 100;
        float capBase = (float) lmrCapBase.value / 100;
        float capDivisor = (float) lmrCapDivisor.value / 100;
        lmrReductions = new int[2][][];

        for (int quiet = 0; quiet < 2; quiet++) {
            lmrReductions[quiet] = new int[Search.MAX_DEPTH][];
            for (int depth = 1; depth < Search.MAX_DEPTH; ++depth) {
                lmrReductions[quiet][depth] = new int[250];
                float base = quiet == 0 ? quietBase : capBase;
                float divisor = quiet == 0 ? quietDivisor : capDivisor;
                for (int movesSearched = 1; movesSearched < 250; ++movesSearched) {
                    lmrReductions[quiet][depth][movesSearched] = (int) Math.round(base + (Math.log(movesSearched) * Math.log(depth) / divisor));
                }
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
