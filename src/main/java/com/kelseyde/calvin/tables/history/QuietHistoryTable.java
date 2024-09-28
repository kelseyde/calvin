package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Colour;
import com.kelseyde.calvin.board.Move;

public class QuietHistoryTable extends HistoryTable {

    private static final int MAX_BONUS = 1200;
    private static final int MAX_SCORE = 8192;

    int[][][][] table = new int[2][Square.COUNT][Square.COUNT][2];

    public void update(Move move, int depth, long threats, boolean white, boolean good) {
        int colourIndex = Colour.index(white);
        int from = move.from();
        int to = move.to();
        int toThreatened = Bits.contains(threats, to) ? 1 : 0;
        int current = table[colourIndex][from][to][toThreatened];
        int bonus = bonus(depth);
        if (!good) bonus = -bonus;
        int update = gravity(current, bonus);
        table[colourIndex][from][to][toThreatened] = update;
    }

    public int get(Move historyMove, long threats, boolean white) {
        int colourIndex = Colour.index(white);
        int from = historyMove.from();
        int to = historyMove.to();
        int toThreatened = Bits.contains(threats, to) ? 1 : 0;
        return table[colourIndex][from][to][toThreatened];
    }

    public void ageScores(boolean white) {
        int colourIndex = Colour.index(white);
        for (int from = 0; from < Square.COUNT; from++) {
            for (int to = 0; to < Square.COUNT; to++) {
                table[colourIndex][from][to][0] /= 2;
                table[colourIndex][from][to][1] /= 2;
            }
        }
    }

    public void clear() {
        table = new int[2][Square.COUNT][Square.COUNT][2];
    }

    @Override
    protected int getMaxScore() {
        return MAX_SCORE;
    }

    @Override
    protected int getMaxBonus() {
        return MAX_BONUS;
    }

}