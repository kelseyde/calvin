package com.kelseyde.calvin.search.moveordering;

import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.generation.Attacks;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.utils.Notation;

public class SEE {

    private static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();

    public static boolean see(Board board, Move move, int threshold) {

        boolean white = board.isWhiteToMove();
        int from = move.getFrom();
        int to = move.getTo();
        Piece nextVictim = move.isPromotion() ? move.getPromotionPiece() : board.pieceAt(from);
        int balance = moveScore(board, move) - threshold;

        if (balance < 0)
            return false;

        balance -= nextVictim.getValue();

        if (balance >= 0)
            return true;

        long bishops = board.getBishops() | board.getQueens();
        long rooks = board.getRooks() | board.getQueens();

        long occ = board.getOccupied();
        occ = (occ ^ (1L << from)) | (1L << to);
        if (move.isEnPassant()) {
            int epFile = board.getGameState().getEnPassantFile();
            int epSquare = toEnPassantSquare(epFile, white);
            occ &= ~(1L << epSquare);
        }

        long attackers = getAttackers(board, to, white) | getAttackers(board, to, !white) & occ;

        white = !white;

        while (true) {
            long myAttackers = attackers & board.getPieces(white);

            // If we have no more attackers left we lose
            if (myAttackers == 0)
                break;

            // Get next least valuable attacker
            nextVictim = getLeastValuableAttacker(board, myAttackers, white, bishops, rooks);

            // Remove this attacker from the occupied
            occ ^= 1L << Bitwise.getNextBit(myAttackers & board.getPieces(nextVictim, white));

            // Make sure we did not add any already used attacks
            attackers &= occ;

            // Swap the turn
            white = !white;

            // Negamax the balance and add the value of the next victim
            balance = -balance - 1 - nextVictim.getValue();

            // As a slide speed up for move legality checking, if our last attacking
            // piece is a king, and our opponent still has attackers, then we've
            // lost as the move we followed would be illegal
            if (balance >= 0) {
                if (nextVictim == Piece.KING & (attackers & board.getPieces(white)) != 0) {
                    white = !white;
                }
                break;
            }

        }

        return board.isWhiteToMove() != white;

    }

    public static int moveScore(Board board, Move move) {

        if (move.isPromotion()) {
            return move.getPromotionPiece().getValue() - Piece.PAWN.getValue();
        }
        else if (move.isEnPassant()) {
            return Piece.PAWN.getValue();
        }
        else {
            Piece targetPiece = board.pieceAt(move.getTo());
            return targetPiece != null ? targetPiece.getValue() : 0;
        }

    }

    private static Piece getLeastValuableAttacker(Board board, long myAttackers, boolean white, long bishops, long rooks) {
        Piece nextVictim;
        if ((myAttackers & board.getPawns(white)) != 0) {
            nextVictim = Piece.PAWN;
        }
        else if ((myAttackers & board.getKnights(white)) != 0) {
            nextVictim = Piece.KNIGHT;
        }
        else if ((myAttackers & bishops) != 0) {
            nextVictim = Piece.BISHOP;
        }
        else if ((myAttackers & rooks) != 0) {
            nextVictim = Piece.ROOK;
        }
        else if ((myAttackers & board.getQueens(white)) != 0) {
            nextVictim = Piece.QUEEN;
        }
        else if ((myAttackers & board.getKing(white)) != 0) {
            nextVictim = Piece.KING;
        }
        else {
            throw new IllegalArgumentException("Invalid piece type");
        }
        return nextVictim;
    }

    private static long getAttackers(Board board, int square, boolean white) {
        return MOVE_GENERATOR.getPawnAttacks(board, square, !white) & board.getPawns(white) |
                MOVE_GENERATOR.getKnightAttacks(board, square, white) & board.getKnights(white) |
                MOVE_GENERATOR.getBishopAttacks(board, square, white) & board.getBishops(white) |
                MOVE_GENERATOR.getRookAttacks(board, square, white) & board.getRooks(white) |
                MOVE_GENERATOR.getQueenAttacks(board, square, white) & board.getQueens(white) |
                MOVE_GENERATOR.getKingAttacks(board, square, white) & board.getKing(white);
    }


    private static int toEnPassantSquare(int enPassantFile, boolean white) {
        int rank = white ? 2 : 5;
        if (enPassantFile == -1) {
            return -1;
        }
        return Board.squareIndex(rank, enPassantFile);
    }

}
