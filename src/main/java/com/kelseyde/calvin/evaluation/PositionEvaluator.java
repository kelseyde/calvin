package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Board;

/**
 * Assigns the current position a numeric value based on some consideration.
 */
public interface PositionEvaluator {

    int evaluate(Board board);

}
