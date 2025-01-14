package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Colour;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineConfig;

public class CaptureHistoryTable extends AbstractHistoryTable {

    int[][][][] table = new int[2][Piece.COUNT][Square.COUNT][Piece.COUNT];

    public CaptureHistoryTable(EngineConfig config) {
        super(config.captHistBonusMax.value,
                config.captHistBonusScale.value,
                config.captHistBonusOffset.value,
                config.captHistMalusMax.value,
                config.captHistMalusScale.value,
                config.captHistMalusOffset.value,
                config.captHistMaxScore.value);
    }

    public void update(Piece piece, int to, Piece captured, int depth, boolean white, boolean good) {
        int colourIndex = Colour.index(white);
        int pieceIndex = piece.index();
        int capturedIndex = captured.index();
        int current = table[colourIndex][pieceIndex][to][capturedIndex];
        int bonus = good ? bonus(depth) : malus(depth);
        int update = gravity(current, bonus);
        table[colourIndex][pieceIndex][to][capturedIndex] = update;
    }

    public int get(Piece piece, int to, Piece captured, boolean white) {
        int colourIndex = Colour.index(white);
        int pieceIndex = piece.index();
        int capturedIndex = captured.index();
        return table[colourIndex][pieceIndex][to][capturedIndex];
    }

    public void clear() {
        table = new int[2][Piece.COUNT][Square.COUNT][Piece.COUNT];
    }

}
