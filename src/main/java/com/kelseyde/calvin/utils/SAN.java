package com.kelseyde.calvin.utils;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.generation.MoveGenerator;

import java.util.List;

public class SAN {

    /**
     * Convert move to Standard Algebraic Notation (SAN)
     * Note: the move must not yet have been made on the board
     */
    public static String fromMove(Move move, Board board) {
        Piece pieceType = board.pieceAt(move.getFrom());
        Piece capturedPieceType = board.pieceAt(move.getTo());

        if (move.isCastling()) {
            int delta = move.getTo() - move.getFrom();
            return delta == 2 ? "O-O" : "O-O-O";
        }

        MoveGenerator moveGenerator = new MoveGenerator();
        String notation = "";
        if (pieceType != Piece.PAWN) {
            notation += Notation.PIECE_CODE_INDEX.get(pieceType).toUpperCase();
        }

        // Check if any ambiguity exists in notation (e.g. if e2 can be reached via Nfe2 and Nbe2)
        if (pieceType != Piece.PAWN && pieceType != Piece.KING) {
            List<Move> legalMoves = moveGenerator.generateMoves(board);

            for (Move legalMove : legalMoves) {

                if (legalMove.getFrom() != move.getFrom() && legalMove.getTo() == move.getTo()) {
                    if (board.pieceAt(legalMove.getFrom()) == pieceType) {
                        int fromFileIndex = Board.file(move.getFrom());
                        int alternateFromFileIndex = Board.file(legalMove.getTo());
                        int fromRankIndex = Board.rank(move.getFrom());
                        int alternateFromRankIndex = Board.rank(legalMove.getFrom());

                        if (fromFileIndex != alternateFromFileIndex) {
                            notation += Notation.getFileChar(move.getFrom());
                            break;
                        }
                        else if (fromRankIndex != alternateFromRankIndex)
                        {
                            notation += Notation.getRankChar(move.getFrom());
                            break;
                        }
                    }
                }
            }
        }

        if (capturedPieceType != null) {
            // add 'x' to indicate capture
            if (pieceType == Piece.PAWN) {
                notation += Notation.getFileChar(move.getFrom());
            }
            notation += "x";
        }
        else {
            // Check if capturing en passant
            if (move.isEnPassant()) {
                notation += Notation.getFileChar(move.getFrom()) + "x";
            }
        }

        notation += Notation.getFileChar(move.getTo());
        notation += Notation.getRankChar(move.getTo());

        // Add promotion piece type
        if (move.isPromotion()) {
            Piece promotionPieceType = move.getPromotionPiece();
            notation += "=" + Notation.PIECE_CODE_INDEX.get(promotionPieceType).toUpperCase();
        }

        board.makeMove(move);
        if (moveGenerator.isCheck(board, board.isWhiteToMove())) {
            List<Move> legalMoves = moveGenerator.generateMoves(board);
            notation += legalMoves.isEmpty() ? "#" : "+";
        }
        board.unmakeMove();

        return notation;
    }

}
