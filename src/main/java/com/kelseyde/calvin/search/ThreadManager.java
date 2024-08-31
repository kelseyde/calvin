package com.kelseyde.calvin.search;

import com.kelseyde.calvin.evaluation.Score;
import com.kelseyde.calvin.uci.UCI;

public class ThreadManager {

    int depthLogged;

    public ThreadManager() {
        depthLogged = 0;
    }

    public synchronized void handleSearchResult(SearchResult searchResult) {
        if (searchResult.depth() > depthLogged
                && (searchResult.time() > 0 || Score.isMateScore(searchResult.eval()))) {
            UCI.writeSearchInfo(searchResult);
            depthLogged++;
        }
    }

    public synchronized void reset() {
        depthLogged = 0;
    }

}
