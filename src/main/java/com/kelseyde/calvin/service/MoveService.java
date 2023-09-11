package com.kelseyde.calvin.service;

import com.kelseyde.calvin.model.Board;
import com.kelseyde.calvin.model.Colour;
import com.kelseyde.calvin.model.Piece;
import com.kelseyde.calvin.model.game.Game;
import com.kelseyde.calvin.model.move.Move;
import com.kelseyde.calvin.model.move.MoveType;
import com.kelseyde.calvin.service.generator.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Evaluates the effect of a {@link Move} on a game. First checks if the move is legal. Then, checks if executing the
 * move results in the game ending, either due to checkmate or one of the draw conditions.
 */
@Slf4j
public class MoveService {

    private final List<PseudoLegalMoveGenerator> pseudoLegalMoveGenerators = List.of(
            new PawnMoveGenerator(), new KnightMoveGenerator(), new BishopMoveGenerator(),
            new RookMoveGenerator(), new QueenMoveGenerator(), new KingMoveGenerator()
    );

    public Set<Move> generateLegalMoves(Game game) {
        Colour colour = game.getTurn();
        Set<Integer> pieceSquares = game.getBoard().getPiecePositions(colour);
        int opponentKingSquare = game.getBoard().getKingSquare(colour.oppositeColour());

        return pieceSquares.stream()
                .flatMap(square -> generatePseudoLegalMoves(game, colour, square).stream())
                .filter(pseudoLegalMove -> !isKingCapturable(game, pseudoLegalMove))
                .map(legalMove -> calculateCheck(game, legalMove))
                .collect(Collectors.toSet());
    }

    public Set<Move> generateAllPseudoLegalMoves(Game game, Colour colour) {
        Set<Integer> pieceSquares = game.getBoard().getPiecePositions(colour);
        return pieceSquares.stream()
                .map(square -> generatePseudoLegalMoves(game, colour, square))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<Move> generatePseudoLegalMoves(Game game, Colour colour, int square) {

        Piece piece = game.getBoard().pieceAt(square)
                .filter(p -> p.getColour().isSameColour(colour))
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("No piece of colour %s on square %s!", colour, square)));

        PseudoLegalMoveGenerator pseudoLegalMoveGenerator = pseudoLegalMoveGenerators.stream()
                .filter(generator -> piece.getType().equals(generator.getPieceType()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("No move generator found for piece type %s!", piece.getType())));

        return pseudoLegalMoveGenerator.generatePseudoLegalMoves(game, square);

    }

    private boolean isKingCapturable(Game game, Move move) {
        Board board = game.getBoard();
        Colour colour = game.getTurn();

        game.applyMove(move);

        Set<Integer> checkableSquares = MoveType.CASTLE.equals(move.getType()) ?
                move.getCastlingConfig().getKingTravelSquares() : Set.of(board.getKingSquare(colour));
        Set<Integer> attackedSquares = generateAllPseudoLegalMoves(game, colour.oppositeColour()).stream()
                .map(Move::getEndSquare)
                .collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(checkableSquares);
        intersection.retainAll(attackedSquares);
        boolean isKingCapturable = !intersection.isEmpty();

        game.unapplyLastMove();

        return isKingCapturable;
    }

    private Move calculateCheck(Game game, Move move) {
        Board board = game.getBoard();
        Colour colour = game.getTurn();

        game.applyMove(move);

        int opponentKingSquare = board.getKingSquare(colour.oppositeColour());
        Set<Integer> attackingSquares = generateAllPseudoLegalMoves(game, colour).stream()
                .map(Move::getEndSquare)
                .collect(Collectors.toSet());
        boolean isCheck = attackingSquares.contains(opponentKingSquare);
        move.setCheck(isCheck);

        game.unapplyLastMove();
        return move;
    }

}
