package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;

public class ContHistTable extends AbstractHistoryTable {

    private static final int MAX_BONUS = 1200;
    private static final int MAX_SCORE = 8192;

    int[][][][][] table = new int[2][6][64][6][64];

    public int get(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return 0;
        }
        int colourIndex = Board.colourIndex(white);
        return table[colourIndex][prevPiece.getIndex()][prevMove.getTo()][currPiece.getIndex()][currMove.getTo()];
    }

    public void set(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, int update, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return;
        }
        int colourIndex = Board.colourIndex(white);
        table[colourIndex][prevPiece.getIndex()][prevMove.getTo()][currPiece.getIndex()][currMove.getTo()] = update;
    }

    public void add(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, int depth, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return;
        }
        int current = get(prevMove, prevPiece, currMove, currPiece, white);
        int bonus = bonus(depth);
        int update = gravity(current, bonus);
        set(prevMove, prevPiece, currMove, currPiece, update, white);
    }

    public void sub(Move prevMove, Piece prevPiece, Move currMove, Piece currPiece, int depth, boolean white) {
        if (prevMove == null || prevPiece == null || currMove == null || currPiece == null) {
            return;
        }
        int current = get(prevMove, prevPiece, currMove, currPiece, white);
        int bonus = bonus(depth);
        int update = gravity(current, -bonus);
        set(prevMove, prevPiece, currMove, currPiece, update, white);
    }

    public void clear() {
        table = new int[2][6][64][6][64];
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
