package com.kelseyde.calvin.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelseyde.calvin.model.*;
import com.kelseyde.calvin.model.move.Move;
import com.kelseyde.calvin.service.game.DrawService;
import com.kelseyde.calvin.service.game.LegalMoveService;
import com.kelseyde.calvin.utils.BoardUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Represents a full game of chess, capturing the current and previous board state, as well as all other metadata required
 * to play the game (castling rights, en passant square, etc). Essentially a statemachine that carries the game from the
 * first move to the end result.
 */
@Data
@Slf4j
public class Game {

    private String id = UUID.randomUUID().toString();

    private Board board;

    private Stack<Board> boardHistory = new Stack<>();
    private Stack<Move> moveHistory = new Stack<>();
    private Set<Move> legalMoves = new HashSet<>();

    @JsonIgnore
    private LegalMoveService legalMoveService = new LegalMoveService();
    @JsonIgnore
    private DrawService drawService = new DrawService();

    public Game() {
        this.board = BoardUtils.startingPosition();
        this.legalMoves = legalMoveService.generateLegalMoves(this);
    }

    public Game(Board board) {
        this.board = board;
        this.legalMoves = legalMoveService.generateLegalMoves(this);
    }

    public ActionResult executeAction(GameAction action) {
        return switch (action.getActionType()) {
            case MOVE -> handleMove(action.getMove());
            case RESIGN -> handleResignation(action.getPlayer());
        };
    }

    public ActionResult handleMove(Move move) {

        Optional<Move> legalMove = legalMoves.stream().filter(move::moveMatches).findAny();
        if (legalMove.isEmpty()) {
            return ActionResult.builder()
                    .isValidMove(false)
                    .build();
        }

        move = legalMove.get();

        applyMove(move);

        handleEnPassantRights(move);
        handleCastlingRights(move);
        handleMoveCounters(move);

        moveHistory.push(move);
        board.setTurn(board.getTurn().oppositeColour());
        legalMoves = legalMoveService.generateLegalMoves(this);

        if (move.isCheck() && legalMoves.isEmpty()) {
            return ActionResult.builder()
                    .isWin(true)
                    .winningColour(board.getTurn().oppositeColour())
                    .winType(WinType.CHECKMATE)
                    .build();
        } else {
            Optional<DrawType> drawType = drawService.calculateDraw(this);
            if (drawType.isPresent()) {
                return ActionResult.builder()
                        .isDraw(true)
                        .drawType(drawType.get())
                        .build();
            } else {
                return ActionResult.builder()
                        .sideToPlay(board.getTurn())
                        .build();
            }
        }

    }

    public void applyMove(Move move) {
        boardHistory.push(board.copy());
        Piece piece = board.getPieceAt(move.getStartSquare()).orElseThrow();
        board.unsetPiece(move.getStartSquare());
        board.setPiece(move.getEndSquare(), piece);

        switch (move.getMoveType()) {
            case EN_PASSANT -> {
                board.unsetPiece(move.getEnPassantCapturedSquare());
            }
            case PROMOTION -> {
                Piece promotedPiece = new Piece(piece.getColour(), move.getPromotionPieceType());
                board.setPiece(move.getEndSquare(), promotedPiece);
            }
            case CASTLE -> {
                Piece rook = board.getPieceAt(move.getRookStartSquare()).orElseThrow();
                board.unsetPiece(move.getRookStartSquare());
                board.setPiece(move.getRookEndSquare(), rook);
            }
        }
    }

    public void unapplyLastMove() {
        if (!boardHistory.isEmpty()) {
            board = boardHistory.pop();
        }
        if (!moveHistory.isEmpty()) {
            moveHistory.pop();
        }
    }

    public Colour getTurn() {
        return board.getTurn();
    }

    public void setTurn(Colour turn) {
        this.board.setTurn(turn);
        this.legalMoves = legalMoveService.generateLegalMoves(this);
    }

    private void handleEnPassantRights(Move move) {
        board.setEnPassantTargetSquare(move.getEnPassantTargetSquare());
    }

    private void handleCastlingRights(Move move) {
        if (move.isNegatesKingsideCastling()) {
            board.getCastlingRights().get(board.getTurn()).setKingSide(false);
        }
        if (move.isNegatesQueensideCastling()) {
            board.getCastlingRights().get(board.getTurn()).setQueenSide(false);
        }
    }

    private void handleMoveCounters(Move move) {
        if (Colour.BLACK.equals(board.getTurn())) {
            board.incrementMoveCounter();
        }
        boolean resetHalfMoveClock = move.isCapture() || PieceType.PAWN.equals(move.getPieceType());
        if (resetHalfMoveClock) {
            board.resetHalfMoveCounter();
        } else {
            board.incrementHalfMoveCounter();
        }
    }

    private ActionResult handleResignation(Player player) {
        return ActionResult.builder()
                .isWin(true)
                .winningColour(player.getColour().oppositeColour())
                .winType(WinType.RESIGNATION)
                .build();
    }


}
