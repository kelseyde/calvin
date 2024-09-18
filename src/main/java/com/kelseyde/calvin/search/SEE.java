package com.kelseyde.calvin.search;

import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.generation.MoveGenerator;

/**
 * SEE, or 'Static Exchange Evaluation' function, calculates the change in material balance after a series of exchanges
 * on a single square. Very similar to the human player's heuristic of 'counting the attackers and defenders', it returns
 * an int value signifying the material loss or gain if all possible attackers and defenders of that square are traded
 * away.
 * Used in the quiescence search to prune capture nodes which are obviously detrimental to the side to move (like
 * exchanging your queen for a pawn). This improves search speed at the cost of potentially missing some tactical complications
 * elsewhere on the board.
 *
 * @see <a href="https://www.chessprogramming.org/Static_Exchange_Evaluation">Chess Programming Wiki</a>
 */
public class SEE {

    private static final MoveGenerator MOVEGEN = new MoveGenerator();

    public static int see(Board board, Move move) {

        int score = 0;
        int square = move.to();
        Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(square);
        score += captured != null ? captured.getValue() : 0;

        board.makeMove(move);
        Move leastValuableAttacker = getLeastValuableAttacker(board, square);
        if (leastValuableAttacker != null) {
            /* The opponent should have the option of 'standing pat' - that is, declining to continue the capture
             sequence if it would lead to a loss of material.
             Therefore, we return the minimum of the stand-pat score and the capture score. */
            score = Math.min(score, score - see(board, leastValuableAttacker));
        }
        board.unmakeMove();
        return score;

    }


    private static Move getLeastValuableAttacker(Board board, int square) {

        boolean white = board.isWhite();

        long pawns = board.getPawns(white);
        if (pawns > 0) {
            long pawnAttackMask = MOVEGEN.getPawnAttacks(board, square, !white);
            if ((pawnAttackMask & pawns) != 0) {
                int pawnStartSquare = Bitwise.getNextBit(pawnAttackMask & pawns);
                return new Move(pawnStartSquare, square);
            }
        }

        long knights = board.getKnights(white);
        if (knights > 0) {
            long knightAttackMask = MOVEGEN.getKnightAttacks(board, square, !white);
            if ((knightAttackMask & knights) != 0) {
                int knightStartSquare = Bitwise.getNextBit(knightAttackMask & knights);
                return new Move(knightStartSquare, square);
            }
        }

        long bishops = board.getBishops(white);
        if (bishops > 0) {
            long bishopAttackMask = MOVEGEN.getBishopAttacks(board, square, !white);
            if ((bishopAttackMask & bishops) != 0) {
                int bishopStartSquare = Bitwise.getNextBit(bishopAttackMask & bishops);
                return new Move(bishopStartSquare, square);
            }
        }

        long rooks = board.getRooks(white);
        if (rooks > 0) {
            long rookAttackMask = MOVEGEN.getRookAttacks(board, square, !white);
            if ((rookAttackMask & rooks) != 0) {
                int rookStartSquare = Bitwise.getNextBit(rookAttackMask & rooks);
                return new Move(rookStartSquare, square);
            }
        }

        long queens = board.getQueens(white);
        if (queens > 0) {
            long queenAttackMask = MOVEGEN.getQueenAttacks(board, square, !white);
            if ((queenAttackMask & queens) != 0) {
                int queenStartSquare = Bitwise.getNextBit(queenAttackMask & queens);
                return new Move(queenStartSquare, square);
            }
        }

        long king = board.getKing(white);
        long kingAttackMask = MOVEGEN.getKingAttacks(board, square, !white);
        if ((kingAttackMask & king) != 0) {
            int kingStartSquare = Bitwise.getNextBit(kingAttackMask & king);
            return new Move(kingStartSquare, square);
        }
        return null;
    }

}
