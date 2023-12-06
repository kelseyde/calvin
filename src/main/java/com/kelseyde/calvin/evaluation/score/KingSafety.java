package com.kelseyde.calvin.evaluation.score;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.utils.BoardUtils;
import com.kelseyde.calvin.utils.Distance;

/**
 * King safety is a core feature in the evaluation function. An unsafe king often outweighs the material balance in
 * deciding the outcome of a game. In this evaluation function the king is penalised for being uncastled; for being
 * castled but with the pawns infront of the king (the 'pawn shield') pushed too far up the board; or for having open
 * or semi-open files either infront of or adjacent to the king.
 * </p>
 * The king safety is tapered off towards the endgame, since it matters less when there are fewer pieces on the board,
 * and the king becomes a more active piece.
 * </p>
 * @see <a href="https://www.chessprogramming.org/King_Safety">Chess Programming Wiki</a>
 */
public class KingSafety {

    public static int score(EngineConfig config, Board board, Material opponentMaterial, float phase, boolean isWhite) {

        if (phase <= 0.5) {
            return 0;
        }
        int kingSquare = Bitwise.getNextBit(board.getKing(isWhite));
        int kingFile = BoardUtils.getFile(kingSquare);

        long friendlyPawns = board.getPawns(isWhite);
        long opponentPawns = board.getPawns(!isWhite);

        int pawnShieldPenalty = calculatePawnShieldPenalty(config, kingSquare, kingFile, friendlyPawns);
        int openKingFilePenalty = calculateOpenKingFilePenalty(config, kingFile, friendlyPawns, opponentPawns, opponentMaterial, isWhite);
        int lostCastlingRightsPenalty = calculateLostCastlingRightsPenalty(config, board, isWhite, kingFile);

        if (Bitwise.countBits(board.getQueens(!isWhite)) == 0) {
            // King safety matters less without opponent queen
            phase *= 0.6f;
        }

        return (int) -((pawnShieldPenalty + openKingFilePenalty + lostCastlingRightsPenalty) * phase);

    }

    public static int calculatePawnShieldPenalty(EngineConfig config, int kingSquare, int kingFile, long pawns) {
        int pawnShieldPenalty = 0;
        if (kingFile <= 2 || kingFile >= 5) {
            long tripleFileMask = Bits.TRIPLE_FILE_MASK[kingFile];

            // Add penalty for a castled king with pawns far away from their starting squares.
            long pawnShieldMask =  tripleFileMask & pawns;
            while (pawnShieldMask != 0) {
                int pawn = Bitwise.getNextBit(pawnShieldMask);
                int distance = Distance.chebyshev(kingSquare, pawn);
                pawnShieldPenalty += config.getKingPawnShieldPenalty()[distance];
                pawnShieldMask = Bitwise.popBit(pawnShieldMask);
            }
        }
        return pawnShieldPenalty;
    }

    public static int calculateOpenKingFilePenalty(EngineConfig config, int kingFile, long friendlyPawns, long opponentPawns, Material opponentMaterial, boolean isWhite) {
        int openKingFilePenalty = 0;
        if (opponentMaterial.rooks() > 0 || opponentMaterial.queens() > 0) {

            for (int attackFile = kingFile - 1; attackFile <= kingFile + 1; attackFile++) {
                if (attackFile < 0 || attackFile > 7) {
                    continue;
                }
                long fileMask = Bits.FILE_MASKS[attackFile];
                boolean isKingFile = attackFile == kingFile;
                boolean isFriendlyPawnMissing = (friendlyPawns & fileMask) == 0;
                boolean isOpponentPawnMissing = (opponentPawns & fileMask) == 0;
                if (isFriendlyPawnMissing || isOpponentPawnMissing) {
                    // Add penalty for semi-open file around the king
                    openKingFilePenalty += isKingFile ? config.getKingSemiOpenFilePenalty() : config.getKingSemiOpenAdjacentFilePenalty();
                }
                if (isFriendlyPawnMissing && isOpponentPawnMissing) {
                    // Add penalty for fully open file around king
                    openKingFilePenalty += isKingFile ? config.getKingOpenFilePenalty() : config.getKingSemiOpenFilePenalty();
                }
            }

        }
        return openKingFilePenalty;
    }

    public static int calculateLostCastlingRightsPenalty(EngineConfig config, Board board, boolean isWhite, int kingFile) {
        if (kingFile <= 2 || kingFile >= 5) {
            return 0;
        }
        boolean hasCastlingRights = board.getGameState().hasCastlingRights(isWhite);
        boolean opponentHasCastlingRights = board.getGameState().hasCastlingRights(!isWhite);
        if (!hasCastlingRights && opponentHasCastlingRights) {
            return config.getKingLostCastlingRightsPenalty();
        }
        return 0;
    }

}
