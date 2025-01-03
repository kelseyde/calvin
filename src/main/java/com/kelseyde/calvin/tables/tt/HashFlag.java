package com.kelseyde.calvin.tables.tt;

public class HashFlag {

    /**
     * The value for this node is the exact evaluation as determined by the evaluation function.
     */
    public static final int EXACT = 0;

    /**
     * This node resulted in a beta-cutoff for the opponent, meaning they discarded the move that resulted in this position
     * because they evaluated it as being worse than other options they had already considered. So, we know there is probably
     * a 'good' move in this position. However, the opponent's search cut off as soon as they found one move matching this
     * criterion, so there may be better moves. The evaluation is therefore the 'lower bound' of the true evaluation.
     */
    public static final int LOWER = 1;

    /**
     * This node was evaluated by the player as being inferior to alternatives they already considered - i.e. it did not
     * improve their alpha score. Basically, everything sucks here. There is no good move for me in the position, *and*
     * I have already discovered an alternative move I can use to avoid reaching this position at all. The node value is
     * an upper bound because it is the maximum score you can hope to achieve from all the moves this position.
     */
    public static final int UPPER = 2;

    public static final int NONE = 3;


}
