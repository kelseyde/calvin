package com.kelseyde.calvin.generation;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGeneration.MoveFilter;
import com.kelseyde.calvin.search.moveordering.MoveOrdering;
import com.kelseyde.calvin.utils.Manager;

import java.util.List;

public class MovePicker {

    public enum Stage {
        PREVIOUS_BEST_MOVE,
        NOISY,
        //KILLERS,
        //QUIET,
        END
    }

    private final MoveGeneration moveGenerator;
    private final MoveOrdering moveOrderer;
    private final Board board;
    private final int ply;

    private Stage stage = Stage.PREVIOUS_BEST_MOVE;
    private MoveFilter filter = MoveFilter.ALL;

    private Move[] killerMoves;
    private int killerIndex;

    private List<Move> moves;
    private Move previousBestMove;
    //private List<Move> quietMoves;
    private int moveIndex;
    private int[] scores;

    public MovePicker(MoveGeneration moveGenerator, MoveOrdering moveOrderer, Board board, int ply) {
        this.moveGenerator = moveGenerator;
        this.moveOrderer = moveOrderer;
        this.board = board;
        this.ply = ply;
    }

    public Move pickNextMove() {

        if (stage == null) {
            stage = Stage.PREVIOUS_BEST_MOVE;
        }

        Move nextMove = null;

        while (nextMove == null) {
            nextMove = switch (stage) {
                case PREVIOUS_BEST_MOVE -> pickHashMove();
                case NOISY -> pickNoisyMove();
                //case KILLERS -> pickKillerMove(ply);
                //case QUIET -> pickQuietMove();
                case END -> null;
            };
            if (stage == Stage.END) break;
        }

        return nextMove;

    }

    private Move pickHashMove() {
        //stage = Stage.KILLERS;
        stage = Stage.NOISY;
        return previousBestMove;
    }

    private Move pickKillerMove(int ply) {
        Move killerMove = moveOrderer.getKillerMove(ply, killerIndex);
        if (killerMove != null) {
            killerIndex++;
        } else {
            stage = Stage.NOISY;
        }
        return killerMove;
    }

    private Move pickNoisyMove() {
        if (moves == null) {
            moves = moveGenerator.generateMoves(board, filter);
            Manager.generated += moves.size();
            if (moves.isEmpty()) {
                stage = Stage.END;
                return null;
            }
            moveIndex = 0;
            scoreMoves();
        }
        if (moveIndex >= moves.size()) {
            stage = Stage.END;
            return null;
        }
        sortMoves();
        Move move = moves.get(moveIndex);
        moveIndex++;
        if (move.equals(previousBestMove)) {
            if (moveIndex >= moves.size()) {
                stage = Stage.END;
                return null;
            }
            move = moves.get(moveIndex);
            moveIndex++;
        }
        return move;
    }

    private void scoreMoves() {
        scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            scores[i] = moveOrderer.scoreMove(board, moves.get(i), previousBestMove, ply);
        }
    }

    private void sortMoves() {
        for (int j = moveIndex + 1; j < moves.size(); j++) {
            int firstScore = scores[moveIndex];
            int secondScore = scores[j];
            if (scores[j] > scores[moveIndex]) {
                Move firstMove = moves.get(moveIndex);
                Move secondMove = moves.get(j);
                scores[moveIndex] = secondScore;
                scores[j] = firstScore;
                moves.set(moveIndex, secondMove);
                moves.set(j, firstMove);
            }
        }
    }

//    private Move pickQuietMove() {
//        return null;
//    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setPreviousBestMove(Move previousBestMove) {
        this.previousBestMove = previousBestMove;
    }

    public void setFilter(MoveFilter filter) {
        this.filter = filter;
    }

}
