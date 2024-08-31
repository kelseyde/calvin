package com.kelseyde.calvin.engine;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EngineConfig {

    int minThreadCount;
    int maxThreadCount;
    int defaultThreadCount;

    int minHashSizeMb;
    int maxHashSizeMb;
    int defaultHashSizeMb;

    boolean ownBookEnabled;
    String ownBookFile;
    int ownBookMaxMoves;

    boolean ownTablebaseEnabled;
    int maxTablebaseSupportedPieces;
    String lichessTablebaseBaseUrl;
    boolean lichessTablebaseDebugEnabled;
    long lichessTablebaseTimeoutMs;

    boolean ponderEnabled;
    int principalVariationLength;

    boolean pondering = false;
    boolean searchCancelled = false;

    int maxDepth;
    int maxPossibleMoves = 250;
    int aspMargin;
    int aspFailMargin;
    int nmpDepth;
    int fpDepth;
    int rfpDepth;
    int lmrDepth;
    float lmrBase;
    float lmrDivisor;
    int lmrMinSearchedMoves;
    int lmpDepth;
    int lmpMultiplier;
    int iirDepth;
    int nmpMargin;
    int dpMargin;
    int[][] lmrReductions;
    int[] fpMargin;
    int[] rfpMargin;

    public void postInitialise() {
        calculateLmrReductions();
    }

    private void calculateLmrReductions() {
        // Credit to Lynx (https://github.com/lynx-chess/Lynx) for this formula for determining the optimal late move reduction depth
        lmrReductions = new int[maxDepth][];
        for (int depth = 1; depth < maxDepth; ++depth) {
            lmrReductions[depth] = new int[maxPossibleMoves];
            for (int movesSearched = 1; movesSearched < maxPossibleMoves; ++movesSearched) {
                lmrReductions[depth][movesSearched] = (int) Math.round(lmrBase + (Math.log(movesSearched) * Math.log(depth) / lmrDivisor));
            }
        }
    }

}
