package com.kelseyde.calvin.generation;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;

import java.util.List;

public interface MoveGeneration {

    List<Move> generateMoves(Board board);

    List<Move> generateMoves(Board board, MoveFilter filter);

    List<Move> generateLegalMoves(Board board, MoveFilter filter);

    boolean isCheck(Board board, boolean white);

    enum MoveFilter {
        ALL,
        NOISY,
        QUIET,
        CAPTURES_ONLY,
    }

}
