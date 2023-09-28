package com.kelseyde.calvin.api.repository;

import com.kelseyde.calvin.search.iterative.IterativeDeepeningSearch;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryEngineRepository implements EngineRepository {

    private final Map<String, IterativeDeepeningSearch> engineMap = new HashMap<>();

    @Override
    public Optional<IterativeDeepeningSearch> getEngine(String boardId) {
        return Optional.ofNullable(engineMap.get(boardId));
    }

    @Override
    public void putEngine(IterativeDeepeningSearch engine) {
        engineMap.put(engine.getBoard().getId(), engine);
    }

}
