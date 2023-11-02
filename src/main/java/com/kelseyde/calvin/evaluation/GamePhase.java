package com.kelseyde.calvin.evaluation;

public class GamePhase {

    private static final int KNIGHT_PHASE = 10;
    private static final int BISHOP_PHASE = 10;
    private static final int ROOK_PHASE = 20;
    private static final int QUEEN_PHASE = 45;
    private static final float TOTAL_PHASE = (KNIGHT_PHASE * 4) + (BISHOP_PHASE * 4) + (ROOK_PHASE * 4) + (QUEEN_PHASE * 2);

    public static float fromMaterial(Material whiteMaterial, Material blackMaterial) {
        int currentMaterial =
            (whiteMaterial.knights() * KNIGHT_PHASE) +
            (blackMaterial.knights() * KNIGHT_PHASE) +
            (whiteMaterial.bishops() * BISHOP_PHASE) +
            (blackMaterial.bishops() * BISHOP_PHASE) +
            (whiteMaterial.rooks() * ROOK_PHASE) +
            (blackMaterial.rooks() * ROOK_PHASE) +
            (whiteMaterial.queens() * QUEEN_PHASE) +
            (blackMaterial.queens() * QUEEN_PHASE);
        return currentMaterial / TOTAL_PHASE;
    }

    public static int taperedEval(int middlegameScore, int endgameScore, float phase) {
        return (int) (phase * middlegameScore) + (int) ((1 - phase) * endgameScore);
    }

}
