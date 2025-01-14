package com.kelseyde.calvin.tables.history;

public abstract class AbstractHistoryTable {

    private final int bonusMax;
    private final int bonusScale;
    private final int bonusOffset;
    private final int malusMax;
    private final int malusScale;
    private final int malusOffset;
    private final int scoreMax;

    public AbstractHistoryTable(int bonusMax, int bonusScale, int bonusOffset,
                                int malusMax, int malusScale, int malusOffset,
                                int scoreMax) {
        this.bonusMax = bonusMax;
        this.bonusScale = bonusScale;
        this.bonusOffset = bonusOffset;
        this.malusMax = malusMax;
        this.malusScale = malusScale;
        this.malusOffset = malusOffset;
        this.scoreMax = scoreMax;
    }

    protected int bonus(int depth) {
        return Math.min(bonusScale * depth - bonusOffset, bonusMax);
    }

    protected int malus(int depth) {
        return -Math.min(malusScale * depth - malusOffset, malusMax);
    }

    protected int gravity(int current, int update) {
        return current + update - current * Math.abs(update) / scoreMax;
    }

}
