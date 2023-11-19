package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Board;

import java.time.Duration;

/**
 * Search for the best move/evaluation (encapsulated in a {@link SearchResult}) within a give time limit.
 * See {@link Searcher} for a concrete implementation, using an iterative deepening approach.
 */
public interface Search {

    void init(Board board);

    void setPosition(Board board);

    SearchResult search(Duration duration);

    void clearHistory();

    void logStatistics();

}
