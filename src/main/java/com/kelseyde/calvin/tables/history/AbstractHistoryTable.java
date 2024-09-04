package com.kelseyde.calvin.tables.history;

public abstract class AbstractHistoryTable {

    protected abstract int getMaxScore();

    protected abstract int getMaxBonus();

    protected int bonus(int depth) {
        return Math.min(16 * depth * depth + 32 * depth + 16, getMaxBonus());
    }

    protected int gravity(int current, int update) {
        return current + update - current * Math.abs(update) / getMaxScore();
    }

}
