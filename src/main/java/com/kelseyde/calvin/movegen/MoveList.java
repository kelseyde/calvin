package com.kelseyde.calvin.movegen;

import com.kelseyde.calvin.board.Move;

import java.util.ArrayList;
import java.util.List;

public class MoveList {

    private static final int INITIAL_CAPACITY = 64;
    private static final int GROWTH_FACTOR = 8;

    private short[] moves;
    private int count;

    public MoveList() {
        this.moves = new short[INITIAL_CAPACITY];
        this.count = 0;
    }

    public MoveList(int capacity) {
        this.moves = new short[capacity];
        this.count = 0;
    }

    public void add(short move) {
        if (count == moves.length) {
            short[] newMoves = new short[moves.length + GROWTH_FACTOR];
            System.arraycopy(moves, 0, newMoves, 0, moves.length);
            moves = newMoves;
        }
        moves[count++] = move;
    }

    public Move get(int index) {
        return new Move(moves[index]);
    }

    public int size() {
        return count;
    }

    public void clear() {
        count = 0;
    }

    public List<Move> toList() {
        List<Move> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Move(moves[i]));
        }
        return list;
    }

}
