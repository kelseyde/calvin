package com.kelseyde.calvin.engine;

import com.kelseyde.calvin.search.Search;

public class EngineConfig {

    public EngineConfig() {
        postInitialise();
    }

    public final int minThreadCount = 1;
    public final int maxThreadCount = 12;
    public final int defaultThreadCount = 1;

    public final int ttSizeMin = 16;
    public final int ttSizeMax = 1024;
    public final int ttSizeDefault = 256;
    public final int ttBucketSize = 4;

    public boolean ponderEnabled = false;

    public boolean pondering = false;
    public boolean searchCancelled = false;

    public final int aspMargin = 25;
    public final int aspFailMargin = 150;
    public final int aspMaxReduction = 3;
    public final int nmpDepth = 0;
    public final int fpDepth = 6;
    public final int rfpDepth = 5;
    public final int lmrDepth = 2;
    public final float lmrBase = 0.85f;
    public final float lmrDivisor = 3.12f;
    public final int lmrMinSearchedMoves = 3;
    public final int lmpDepth = 2;
    public final int lmpMultiplier = 10;
    public final int iirDepth = 4;
    public final int nmpMargin = 70;
    public final int dpMargin = 140;
    public final int qsFpMargin = 100;
    public final int fpMargin = 275;
    public final int fpScale = 65;
    public final int[] rfpMargin = { 74, 40 };
    public int[][] lmrReductions;

    public void postInitialise() {
        calculateLmrReductions();
    }

    private void calculateLmrReductions() {
        lmrReductions = new int[Search.MAX_DEPTH][];
        for (int depth = 1; depth < Search.MAX_DEPTH; ++depth) {
            lmrReductions[depth] = new int[250];
            for (int movesSearched = 1; movesSearched < 250; ++movesSearched) {
                lmrReductions[depth][movesSearched] = (int) Math.round(lmrBase + (Math.log(movesSearched) * Math.log(depth) / lmrDivisor));
            }
        }
    }

}
