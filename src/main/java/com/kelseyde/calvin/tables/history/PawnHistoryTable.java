package com.kelseyde.calvin.tables.history;

import com.kelseyde.calvin.board.*;
import com.kelseyde.calvin.engine.EngineConfig;

public class PawnHistoryTable extends AbstractHistoryTable {

    private static final int TABLE_SIZE = 16384;

    private int[][] table = new int[2][TABLE_SIZE];

    public PawnHistoryTable(EngineConfig config) {
        super(config.quietHistBonusMax.value,
                config.quietHistBonusScale.value,
                config.quietHistMalusMax.value,
                config.quietHistMalusScale.value,
                config.quietHistMaxScore.value);
    }

    public int get(Board board, Move move, Piece piece, Piece captured, boolean white) {
        int colourIndex = Colour.index(white);
        long pawnKey = piece == Piece.PAWN ? pawnKey(board, move, piece, captured, white) : board.pawnKey();
        int pawnIndex = hashIndex(pawnKey);
        return table[colourIndex][pawnIndex];
    }

    public void update(Board board, Move move, Piece piece, Piece captured, int depth, boolean white, boolean good) {
        int colourIndex = Colour.index(white);
        long pawnKey = piece == Piece.PAWN ? pawnKey(board, move, piece, captured, white) : board.pawnKey();
        int pawnIndex = hashIndex(pawnKey);
        int current = table[colourIndex][pawnIndex];
        int bonus = good ? bonus(depth) : malus(depth);
        int update = gravity(current, bonus);
        table[colourIndex][pawnIndex] = update;
    }

    public void clear() {
        table = new int[2][TABLE_SIZE];
    }

    private long pawnKey(Board board, Move move, Piece piece, Piece captured, boolean white) {
        long pawnKey = board.pawnKey();
        if (Piece.PAWN != piece && Piece.PAWN != captured) {
            return pawnKey;
        }
        int from = move.from();
        int to = move.to();
        if (move.isPromotion()) {
            return pawnKey ^ Key.piece(from, piece, white);
        }
        else if (move.isEnPassant()) {
            final int pawnSquare = white ? to - 8 : to + 8;
            return pawnKey ^ Key.piece(from, to, piece, white) ^ Key.piece(pawnSquare, Piece.PAWN, !white);
        }
        else {
            if (piece == Piece.PAWN) {
                return pawnKey ^ Key.piece(from, to, piece, white);
            }
            else {
                return pawnKey ^ Key.piece(from, to, captured, !white);
            }
        }
    }

    private int hashIndex(long key) {
        // Ensure the key is positive,
        // then return a modulo of the key and table size.
        return (int) (key & 0x7FFFFFFF) % TABLE_SIZE;
    }

}
