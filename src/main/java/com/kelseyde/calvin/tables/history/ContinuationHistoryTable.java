package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Colour;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineConfig;

public class ContinuationHistoryTable extends AbstractHistoryTable {

    int[][][][][] table = new int[2][Piece.COUNT][Square.COUNT][Piece.COUNT][Square.COUNT];

    public ContinuationHistoryTable(EngineConfig config) {
        super(config.contHistBonusMax.value,
                config.contHistBonusScale.value,
                config.contHistBonusOffset.value,
                config.contHistMalusMax.value,
                config.contHistMalusScale.value,
                config.contHistMalusOffset.value,
                config.contHistMaxScore.value);
    }

    public void update(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, int depth, boolean white, boolean good) {
        int current = get(prevMove, prevPiece, currMove, currPiece, white);
        int bonus = good ? bonus(depth) : malus(depth);
        int update = gravity(current, bonus);
        set(prevMove, prevPiece, currMove, currPiece, update, white);
    }

    public int get(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return 0;
        }
        int colourIndex = Colour.index(white);
        return table[colourIndex][prevPiece.index()][prevMove.to()][currPiece.index()][currMove.to()];
    }

    public void set(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, int update, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return;
        }
        int colourIndex = Colour.index(white);
        table[colourIndex][prevPiece.index()][prevMove.to()][currPiece.index()][currMove.to()] = update;
    }

    public void clear() {
        table = new int[2][Piece.COUNT][Square.COUNT][Piece.COUNT][Square.COUNT];
    }

}
