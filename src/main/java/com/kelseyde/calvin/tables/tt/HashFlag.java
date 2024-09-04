package com.kelseyde.calvin.tables.tt;

public enum HashFlag {

    /**
     * The value for this node is the exact evaluation as determined by the evaluation function.
     */
    EXACT,

    /**
     * This node resulted in a beta-cutoff for the opponent, meaning they discarded the move that resulted in this position
     * because they evaluated it as being worse than other options they had already considered. So, we know there is probably
     * a 'good' move in this position. However, the opponent's search cut off as soon as they found one move matching this
     * criterion, so there may be better moves. The evaluation is therefore the 'lower bound' of the true evaluation.
     */
    LOWER,

    /**
     * This node was evaluated by the player as being inferior to alternatives they already considered - i.e. it did not
     * improve their alpha score. Basically, everything sucks here. There is no good move for me in the position, *and*
     * I have already discovered an alternative move I can use to avoid reaching this position at all. The node value is
     * an upper bound because it is the maximum score you can hope to achieve from all the moves this position.
     */
    UPPER;

    public static int value(HashFlag flag) {
        return switch (flag) {
            case EXACT -> 0;
            case LOWER -> 1;
            case UPPER -> 2;
        };
    }

    public static HashFlag valueOf(int value) {
        return switch (value) {
            case 0 -> EXACT;
            case 1 -> LOWER;
            case 2 -> UPPER;
            default -> throw new IllegalArgumentException("Illegal node type " + value);
        };
    }

}
