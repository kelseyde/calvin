package com.kelseyde.calvin.utils.notation;

import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Bits.Rank;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.movegen.MoveGenerator;

import java.util.List;

public class SAN {

    /**
     * Convert move to Standard Algebraic Notation (SAN)
     * Note: the move must not yet have been made on the board
     */
    public static String fromMove(Move move, Board board) {
        Piece piece = board.pieceAt(move.from());
        Piece captured = board.pieceAt(move.to());

        if (move.isCastling()) {
            int delta = move.to() - move.from();
            return delta == 2 ? "O-O" : "O-O-O";
        }

        MoveGenerator moveGenerator = new MoveGenerator();
        String notation = "";
        if (piece != Piece.PAWN) {
            notation += piece.code().toUpperCase();
        }

        // Check if any ambiguity exists in notation (e.g. if e2 can be reached via Nfe2 and Nbe2)
        if (piece != Piece.PAWN && piece != Piece.KING) {
            List<Move> legalMoves = moveGenerator.generateMoves(board).toList();

            for (Move legalMove : legalMoves) {

                if (legalMove.from() != move.from() && legalMove.to() == move.to()) {
                    if (board.pieceAt(legalMove.from()) == piece) {
                        int fromFileIndex = File.of(move.from());
                        int alternateFromFileIndex = File.of(legalMove.to());
                        int fromRankIndex = Rank.of(move.from());
                        int alternateFromRankIndex = Rank.of(legalMove.from());

                        if (fromFileIndex != alternateFromFileIndex) {
                            notation += File.toFileNotation(move.from());
                            break;
                        }
                        else if (fromRankIndex != alternateFromRankIndex)
                        {
                            notation += Rank.toRankNotation(move.from());
                            break;
                        }
                    }
                }
            }
        }

        if (captured != null) {
            // add 'x' to indicate capture
            if (piece == Piece.PAWN) {
                notation += File.toFileNotation(move.from());
            }
            notation += "x";
        }
        else {
            // Check if capturing en passant
            if (move.isEnPassant()) {
                notation += File.toFileNotation(move.from()) + "x";
            }
        }

        notation += File.toFileNotation(move.to());
        notation += Rank.toRankNotation(move.to());

        // Add promotion piece type
        if (move.isPromotion()) {
            Piece promotionPieceType = move.promoPiece();
            notation += "=" + promotionPieceType.code().toUpperCase();
        }

        board.makeMove(move);
        if (moveGenerator.isCheck(board, board.isWhite())) {
            List<Move> legalMoves = moveGenerator.generateMoves(board).toList();
            notation += legalMoves.isEmpty() ? "#" : "+";
        }
        board.unmakeMove();

        return notation;
    }

}
