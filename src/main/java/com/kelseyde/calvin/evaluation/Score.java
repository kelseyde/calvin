package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Piece;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@NoArgsConstructor
public class Score {

    public static final int MATE_SCORE = 1000000;
    public static final int DRAW_SCORE = 0;
    public static final int[] SIMPLE_PIECE_VALUES = Arrays.stream(Piece.values()).mapToInt(Piece::getValue).toArray();

    public static boolean isMateScore(int eval) {
        return Math.abs(eval) >= Score.MATE_SCORE - 100;
    }

    int whiteMaterialMgScore;
    int whiteMaterialEgScore;
    int blackMaterialMgScore;
    int blackMaterialEgScore;

    int whitePiecePlacementScore;
    int blackPiecePlacementScore;

    int whiteMobilityMgScore;
    int whiteMobilityEgScore;
    int blackMobilityMgScore;
    int blackMobilityEgScore;

    int whitePawnStructureMgScore;
    int whitePawnStructureEgScore;
    int blackPawnStructureMgScore;
    int blackPawnStructureEgScore;

    int whiteKingSafetyScore;
    int blackKingSafetyScore;

    int whiteKnightMgScore;
    int whiteKnightEgScore;
    int blackKnightMgScore;
    int blackKnightEgScore;

    int whiteBishopMgScore;
    int whiteBishopEgScore;
    int blackBishopMgScore;
    int blackBishopEgScore;

    int whiteRookMgScore;
    int whiteRookEgScore;
    int blackRookMgScore;
    int blackRookEgScore;

    int whiteMopUpScore;
    int blackMopUpScore;

    int whiteTempoBonus;
    int blackTempoBonus;

    int scaleFactor = 1;

    float phase;

    public void addMaterialScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteMaterialMgScore += middlegameScore;
            whiteMaterialEgScore += endgameScore;
        } else {
            blackMaterialMgScore += middlegameScore;
            blackMaterialEgScore += endgameScore;
        }
    }

    public void addPiecePlacementScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whitePiecePlacementScore += Phase.taperedEval(middlegameScore, endgameScore, phase);
        } else {
            blackPiecePlacementScore += Phase.taperedEval(middlegameScore, endgameScore, phase);
        }
    }

    public void addMobilityScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteMobilityMgScore += middlegameScore;
            whiteMobilityEgScore += endgameScore;
        } else {
            blackMobilityMgScore += middlegameScore;
            blackMobilityEgScore += endgameScore;
        }
    }

    public void addPawnStructureScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whitePawnStructureMgScore += middlegameScore;
            whitePawnStructureEgScore += endgameScore;
        } else {
            blackPawnStructureMgScore += middlegameScore;
            blackPawnStructureEgScore += endgameScore;
        }
    }

    public void addKnightScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteKnightMgScore += middlegameScore;
            whiteKnightEgScore += endgameScore;
        } else {
            blackKnightMgScore += middlegameScore;
            blackKnightEgScore += endgameScore;
        }
    }

    public void addBishopScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteBishopMgScore += middlegameScore;
            whiteBishopEgScore += endgameScore;
        } else {
            blackBishopMgScore += middlegameScore;
            blackBishopEgScore += endgameScore;
        }
    }

    public void addRookScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteRookMgScore += middlegameScore;
            whiteRookEgScore += endgameScore;
        } else {
            blackRookMgScore += middlegameScore;
            blackRookEgScore += endgameScore;
        }
    }

    public void setKingSafetyScore(int score, boolean white) {
        if (white) {
            whiteKingSafetyScore = score;
        } else {
            blackKingSafetyScore = score;
        }
    }

    public void setMopUpScore(int score, boolean white) {
        if (white) {
            whiteMopUpScore = score;
        } else {
            blackMopUpScore = score;
        }
    }

    public int sum(boolean white) {

        int whiteMaterialScore = Phase.taperedEval(whiteMaterialMgScore, whiteMaterialEgScore, phase);
        int blackMaterialScore = Phase.taperedEval(blackMaterialMgScore, blackMaterialEgScore, phase);

        int whiteMobilityScore = Phase.taperedEval(whiteMobilityMgScore, whiteMobilityEgScore, phase);
        int blackMobilityScore = Phase.taperedEval(blackMobilityMgScore, blackMobilityEgScore, phase);

        int whitePawnStructureScore = Phase.taperedEval(whitePawnStructureMgScore, whitePawnStructureEgScore, phase);
        int blackPawnStructureScore = Phase.taperedEval(blackPawnStructureMgScore, blackPawnStructureEgScore, phase);

        int whiteKnightScore = Phase.taperedEval(whiteKnightMgScore, whiteKnightEgScore, phase);
        int blackKnightScore = Phase.taperedEval(blackKnightMgScore, blackKnightEgScore, phase);

        int whiteBishopScore = Phase.taperedEval(whiteBishopMgScore, whiteBishopEgScore, phase);
        int blackBishopScore = Phase.taperedEval(blackBishopMgScore, blackBishopEgScore, phase);

        int whiteRookScore = Phase.taperedEval(whiteRookMgScore, whiteRookEgScore, phase);
        int blackRookScore = Phase.taperedEval(blackRookMgScore, blackRookEgScore, phase);

        int whiteScore = whiteMaterialScore + whitePiecePlacementScore + whiteMobilityScore + whitePawnStructureScore +
                whiteKingSafetyScore + whiteKnightScore + whiteBishopScore + whiteRookScore + whiteMopUpScore + whiteTempoBonus;

        int blackScore = blackMaterialScore + blackPiecePlacementScore + blackMobilityScore + blackPawnStructureScore +
                blackKingSafetyScore + blackKnightScore + blackBishopScore + blackRookScore + blackMopUpScore + blackTempoBonus;

        int score = whiteScore - blackScore;
        int modifier = white ? 1 : -1;
        return score * modifier;

    }

}
