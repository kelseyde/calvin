package com.kelseyde.calvin.movegeneration.result;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.GameState;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.movegeneration.MoveGenerator;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ResultCalculator {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    public GameResult calculateResult(Board board) {
        Move[] legalMoves = moveGenerator.generateLegalMoves(board, false);
        return calculateResult(board, legalMoves);
    }

    public GameResult calculateResult(Board board, Move[] legalMoves) {
        if (legalMoves.length == 0) {
            if (moveGenerator.isCheck(board, board.isWhiteToMove())) {
                return board.isWhiteToMove() ? GameResult.BLACK_WINS_BY_CHECKMATE : GameResult.WHITE_WINS_BY_CHECKMATE;
            } else {
                return GameResult.DRAW_BY_STALEMATE;
            }
        }
        if (isRepetition(board)) {
            return GameResult.DRAW_BY_REPETITION;
        }
        if (isInsufficientMaterial(board)) {
            return GameResult.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
        if (isFiftyMoveRule(board)) {
            return GameResult.DRAW_BY_FIFTY_MOVE_RULE;
        }
        return GameResult.IN_PROGRESS;
    }

    private boolean isRepetition(Board board) {
        return Stream.concat(board.getGameStateHistory().stream(), Stream.of(board.getGameState()))
                .collect(Collectors.groupingBy(GameState::toRepetitionString))
                .values()
                .stream()
                .anyMatch(repetitions -> repetitions.size() == 3);
    }

    private boolean isInsufficientMaterial(Board board) {
        if (board.getWhitePawns() != 0 || board.getWhiteRooks() != 0 || board.getWhiteQueens() != 0
                || board.getBlackPawns() != 0 || board.getBlackRooks() != 0 || board.getBlackQueens() != 0) {
            return false;
        }
        long whitePieces = board.getWhiteKnights() | board.getWhiteBishops();
        long blackPieces = board.getBlackKnights() |  board.getBlackBishops();

        return (Long.bitCount(whitePieces) == 0 || Long.bitCount(whitePieces) == 1)
                && (Long.bitCount(blackPieces) == 0 || Long.bitCount(blackPieces) == 1);
    }

    private boolean isFiftyMoveRule(Board board) {
        return board.getGameState().getFiftyMoveCounter() >= 100;
    }

}
