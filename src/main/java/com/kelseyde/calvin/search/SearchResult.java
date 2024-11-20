package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Move;

import java.time.Duration;
import java.time.Instant;

public record SearchResult(int eval, Move move, int depth, long time, int nodes, long nps, Move[] pv) {

    public static SearchResult of(Move move) {
        return new SearchResult(0, move, 0, 0, 0, 0, new Move[0]);
    }

    public static SearchResult of(Move move, int score, ThreadData td, TimeControl tc) {
        long millis = tc.start() != null ? Duration.between(tc.start(), Instant.now()).toMillis() : 0;
        long nps = td.nodes > 0 && millis > 0 ? ((td.nodes / millis) * 1000) : 0;
        Move[] pv = td.pv.getPrincipalVariation();
        return new SearchResult(score, move, td.depth, millis, td.nodes, nps, pv);
    }

}
