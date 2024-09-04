package com.kelseyde.calvin.search.moveordering;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.search.SearchStack;

import java.util.List;

/**
 * In order for a chess engine's search algorithm to perform well, the best moves need to be searched first. We don't know
 * for certain the best move at the beginning of the search, but there are several heuristics we can use to approximate
 * which moves are likely to be good and therefore searching early.
 *
 * @see <a href="https://www.chessprogramming.org/Move_Ordering">Chess Programming Wiki</a>
 */
public interface MoveOrdering {

    /**
     * Orders the list of moves based on heuristics to determine the likely best moves.
     *
     * @param board            The current board position.
     * @param moves            The list of possible moves to be ordered.
     * @param previousBestMove The best move from the previous iteration, used as a heuristic.
     * @param depth            The current depth of the search.
     * @return The list of moves ordered by their estimated quality.
     */
    List<Move> orderMoves(Board board, SearchStack ss, List<Move> moves, Move previousBestMove, int depth);

    /**
     * Scores a single move based on heuristics to determine its likely quality.
     *
     * @param board            The current board position.
     * @param move             The move to be scored.
     * @param previousBestMove The best move from the previous iteration, used as a heuristic.
     * @param depth            The current depth of the search.
     * @return The score representing the quality of the move.
     */
    int scoreMove(Board board, SearchStack ss, Move move, Move previousBestMove, int depth);

    /**
     * Scores a move using the Most Valuable Victim - Least Valuable Aggressor (MVV-LVA) heuristic.
     *
     * @param board The current board position.
     * @param move The move to be scored.
     * @return The MVV-LVA score of the move.
     */
    int mvvLva(Board board, Move move, Move previousBestMove);

    /**
     * Adds a killer move for a given ply (depth) in the search.
     *
     * @param ply The depth at which the move is considered a killer move.
     * @param newKiller The move to be added as a killer move.
     */
    void addKillerMove(int ply, Move newKiller);

    /**
     * Adds a move to the history heuristic table.
     *
     * @param historyMove The move to be added to the history table.
     * @param ss          The current search stack.
     * @param depth       The remaining depth in the search.
     * @param ply         The current ply in the search.
     * @param white       Whether the move was made by the white player.
     */
    void addHistoryScore(Move historyMove, SearchStack ss, int depth, int ply, boolean white);

    void subHistoryScore(Move historyMove, SearchStack ss, int depth, int ply, boolean white);

    void ageHistoryScores(boolean white);

    /**
     * Clears all stored data related to move ordering heuristics.
     */
    void clear();

}
