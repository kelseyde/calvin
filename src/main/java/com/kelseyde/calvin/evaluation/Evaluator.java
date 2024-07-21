package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.*;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.generation.Attacks;
import com.kelseyde.calvin.transposition.pawn.PawnHashEntry;
import com.kelseyde.calvin.transposition.pawn.PawnHashTable;
import com.kelseyde.calvin.utils.BoardUtils;
import com.kelseyde.calvin.utils.Distance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Evaluates the current board position. Uses various heuristics to calculate a numeric value, in centipawns, estimating
 * how good the current position is for the side to move. This means that a positive score indicates the position is better
 * for the side to move, regardless of whether they are white or black.
 * <p>
 * Also includes logic for incrementally updating the evaluation during make/unmake move, which saves some time during the
 * search procedure.
 * @see <a href="https://www.chessprogramming.org/Evaluation">Chess Programming Wiki</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Evaluator implements Evaluation {

    static final int MINOR_PIECE_ATTACK_UNIT = 2;
    static final int ROOK_ATTACK_UNIT = 3;
    static final int QUEEN_ATTACK_UNIT = 5;

    final EngineConfig config;
    final PawnHashTable pawnHash;

    final int pawnMgValue;
    final int knightMgValue;
    final int bishopMgValue;
    final int rookMgValue;
    final int queenMgValue;
    final int pawnEgValue;
    final int knightEgValue;
    final int bishopEgValue;
    final int rookEgValue;
    final int queenEgValue;

    final int[] pawnMgTable;
    final int[] pawnEgTable;
    final int[] knightMgTable;
    final int[] knightEgTable;
    final int[] bishopMgTable;
    final int[] bishopEgTable;
    final int[] rookMgTable;
    final int[] rookEgTable;
    final int[] queenMgTable;
    final int[] queenEgTable;
    final int[] kingMgTable;
    final int[] kingEgTable;

    final int[] knightMgMobility;
    final int[] knightEgMobility;
    final int[] bishopMgMobility;
    final int[] bishopEgMobility;
    final int[] rookMgMobility;
    final int[] rookEgMobility;
    final int[] queenMgMobility;
    final int[] queenEgMobility;

    long whiteKingSafetyZone;
    long blackKingSafetyZone;

    int whiteMgScore;
    int blackMgScore;

    int whiteEgScore;
    int blackEgScore;

    int whiteKingAttackZoneUnits;
    int blackKingAttackZoneUnits;

    float phase;

    public Evaluator(EngineConfig config) {
        this(config, new PawnHashTable(config.getDefaultPawnHashSizeMb()));
    }

    public Evaluator(EngineConfig config, PawnHashTable pawnHash) {
        this.config = config;
        this.pawnHash = pawnHash;

        pawnMgValue = config.getPieceValues()[0][Piece.PAWN.getIndex()];
        knightMgValue = config.getPieceValues()[0][Piece.KNIGHT.getIndex()];
        bishopMgValue = config.getPieceValues()[0][Piece.BISHOP.getIndex()];
        rookMgValue = config.getPieceValues()[0][Piece.ROOK.getIndex()];
        queenMgValue = config.getPieceValues()[0][Piece.QUEEN.getIndex()];
        pawnEgValue = config.getPieceValues()[1][Piece.PAWN.getIndex()];
        knightEgValue = config.getPieceValues()[1][Piece.KNIGHT.getIndex()];
        bishopEgValue = config.getPieceValues()[1][Piece.BISHOP.getIndex()];
        rookEgValue = config.getPieceValues()[1][Piece.ROOK.getIndex()];
        queenEgValue = config.getPieceValues()[1][Piece.QUEEN.getIndex()];

        pawnMgTable = config.getMiddlegameTables()[Piece.PAWN.getIndex()];
        pawnEgTable = config.getEndgameTables()[Piece.PAWN.getIndex()];
        knightMgTable = config.getMiddlegameTables()[Piece.KNIGHT.getIndex()];
        knightEgTable = config.getEndgameTables()[Piece.KNIGHT.getIndex()];
        bishopMgTable = config.getMiddlegameTables()[Piece.BISHOP.getIndex()];
        bishopEgTable = config.getEndgameTables()[Piece.BISHOP.getIndex()];
        rookMgTable = config.getMiddlegameTables()[Piece.ROOK.getIndex()];
        rookEgTable = config.getEndgameTables()[Piece.ROOK.getIndex()];
        queenMgTable = config.getMiddlegameTables()[Piece.QUEEN.getIndex()];
        queenEgTable = config.getEndgameTables()[Piece.QUEEN.getIndex()];
        kingMgTable = config.getMiddlegameTables()[Piece.KING.getIndex()];
        kingEgTable = config.getEndgameTables()[Piece.KING.getIndex()];

        knightMgMobility = config.getMiddlegameMobilityBonus()[Piece.KNIGHT.getIndex()];
        knightEgMobility = config.getEndgameMobilityBonus()[Piece.KNIGHT.getIndex()];
        bishopMgMobility = config.getMiddlegameMobilityBonus()[Piece.BISHOP.getIndex()];
        bishopEgMobility = config.getEndgameMobilityBonus()[Piece.BISHOP.getIndex()];
        rookMgMobility = config.getMiddlegameMobilityBonus()[Piece.ROOK.getIndex()];
        rookEgMobility = config.getEndgameMobilityBonus()[Piece.ROOK.getIndex()];
        queenMgMobility = config.getMiddlegameMobilityBonus()[Piece.QUEEN.getIndex()];
        queenEgMobility = config.getEndgameMobilityBonus()[Piece.QUEEN.getIndex()];
    }

    @Override
    public int evaluate(Board board) {

        whiteMgScore = 0;
        whiteEgScore = 0;
        blackMgScore = 0;
        blackEgScore = 0;
        whiteKingAttackZoneUnits = 0;
        blackKingAttackZoneUnits = 0;
        phase = 0;

        boolean white = board.isWhiteToMove();

        long whitePieces = board.getWhitePieces();
        long blackPieces = board.getBlackPieces();

        long whitePawns = board.getPawns(true);
        long whiteKnights = board.getKnights(true);
        long whiteBishops = board.getBishops(true);
        long whiteRooks = board.getRooks(true);
        long whiteQueens = board.getQueens(true);
        long whiteKing = board.getKing(true);
        long blackPawns = board.getPawns(false);
        long blackKnights = board.getKnights(false);
        long blackBishops = board.getBishops(false);
        long blackRooks = board.getRooks(false);
        long blackQueens = board.getQueens(false);
        long blackKing = board.getKing(false);

        whiteKingSafetyZone = Bits.WHITE_KING_SAFETY_ZONE[Bitwise.getNextBit(whiteKing)];
        blackKingSafetyZone = Bits.BLACK_KING_SAFETY_ZONE[Bitwise.getNextBit(blackKing)];

        Material whiteMaterial = Material.fromBoard(board, true);
        Material blackMaterial = Material.fromBoard(board, false);

        phase = Phase.fromMaterial(whiteMaterial, blackMaterial, config);

        int whiteMaterialMgScore = mgMaterialScore(whiteMaterial);
        int whiteMaterialEgScore = egMaterialScore(whiteMaterial);
        addScore(whiteMaterialMgScore, whiteMaterialEgScore, true);

        int blackMaterialMgScore = mgMaterialScore(blackMaterial);
        int blackMaterialEgScore = egMaterialScore(blackMaterial);
        addScore(blackMaterialMgScore, blackMaterialEgScore, false);

        // Blockers used during mobility calculations
        long friendlyWhiteBlockers = whiteKing | whitePawns;
        long friendlyBlackBlockers = blackKing | blackPawns;

        scorePawnsWithHash(board, whitePawns, blackPawns);

        scoreKnights(whiteKnights, friendlyWhiteBlockers, true);
        scoreKnights(blackKnights, friendlyBlackBlockers, false);

        scoreBishops(whiteBishops, friendlyWhiteBlockers, blackPieces, true);
        scoreBishops(blackBishops, friendlyBlackBlockers, whitePieces, false);

        scoreRooks(whiteRooks, whitePawns, blackPawns, friendlyWhiteBlockers, blackPieces, true);
        scoreRooks(blackRooks, blackPawns, whitePawns, friendlyBlackBlockers, whitePieces, false);

        scoreQueens(whiteQueens, friendlyWhiteBlockers, blackPieces, true);
        scoreQueens(blackQueens, friendlyBlackBlockers, whitePieces, false);

        scoreKing(whiteKing, whitePawns, blackPawns, blackMaterial, friendlyWhiteBlockers, blackPieces, board, phase, true);
        scoreKing(blackKing, blackPawns, whitePawns, whiteMaterial, friendlyBlackBlockers, whitePieces, board, phase, false);

        return sum(white);
    }

    @Override
    public void makeMove(Board board, Move move) {

    }

    @Override
    public void unmakeMove() {

    }

    private void scorePawnsWithHash(Board board, long whitePawns, long blackPawns) {

        long pawnKey = board.getGameState().getPawnZobrist();
        PawnHashEntry hashEntry = pawnHash.get(pawnKey);
        int whiteScore;
        int blackScore;
        if (hashEntry != null) {
            whiteScore = hashEntry.whiteScore();
            blackScore = hashEntry.blackScore();
        } else {
            whiteScore = scorePawns(whitePawns, blackPawns, true);
            blackScore = scorePawns(blackPawns, whitePawns, false);
            hashEntry = PawnHashEntry.of(pawnKey, whiteScore, blackScore);
            pawnHash.put(pawnKey, hashEntry);
        }
        addScore(PawnHashEntry.mgScore(whiteScore), PawnHashEntry.egScore(whiteScore), true);
        addScore(PawnHashEntry.mgScore(blackScore), PawnHashEntry.egScore(blackScore), false);

    }

    /**
     * Pawn evaluation consists of piece-placement eval + pawn structure considerations (bonuses for passed pawns,
     * penalties for isolated/doubled pawns).
     */
    private int scorePawns(long friendlyPawns, long opponentPawns, boolean white) {

        // TODO what happens if pawns = 0?
        if (friendlyPawns == 0) return 0;
        int mgScore = 0;
        int egScore = 0;

        int isolatedPawnCount = 0;
        int doubledPawnCount = 0;

        long pawnsIterator = friendlyPawns;
        while (pawnsIterator > 0) {
            int pawn = Bitwise.getNextBit(pawnsIterator);
            int file = BoardUtils.getFile(pawn);

            int square = white ? pawn ^ 56 : pawn;
            mgScore += pawnMgTable[square];
            egScore += pawnEgTable[square];

            // Bonuses for a passed pawn, indexed by the number of squares away that pawn is from promotion.
            // Bonus for a passed pawn that is additionally protected by another pawn (multiplied by number of defending pawns).
            if (Bitwise.isPassedPawn(pawn, opponentPawns, white)) {
                int rank = BoardUtils.getRank(pawn);
                int squaresFromPromotion = white ? 7 - rank : rank;
                mgScore += config.getPassedPawnBonus()[0][squaresFromPromotion];
                egScore += config.getPassedPawnBonus()[1][squaresFromPromotion];

                int numberOfProtectors = Bitwise.countPawnProtectors(pawn, friendlyPawns, white);
                int protectedPawnBonus = numberOfProtectors * config.getProtectedPassedPawnBonus();
                mgScore += protectedPawnBonus;
                egScore += protectedPawnBonus;
            }
            // Passed pawns are not penalised for being isolated
            else if (Bitwise.isIsolatedPawn(file, friendlyPawns)) {
                isolatedPawnCount++;
            }
            if (Bitwise.isDoubledPawn(file, friendlyPawns)) {
                doubledPawnCount++;
            }

            pawnsIterator = Bitwise.popBit(pawnsIterator);
        }

        // Penalties for isolated pawns, indexed by the number of isolated pawns.
        mgScore += config.getIsolatedPawnPenalty()[0][isolatedPawnCount];
        egScore += config.getIsolatedPawnPenalty()[1][isolatedPawnCount];

        // Penalties for doubled pawns, indexed by the number of doubled pawns
        mgScore += config.getDoubledPawnPenalty()[0][doubledPawnCount];
        egScore += config.getDoubledPawnPenalty()[1][doubledPawnCount];

        return PawnHashEntry.encode(mgScore, egScore);
    }

    /**
     * Knight evaluation consists of simple piece-placement and mobility bonuses.
     */
    private void scoreKnights(long knights, long friendlyBlockers, boolean white) {

        if (knights == 0) return;
        int mgScore = 0;
        int egScore = 0;

        while (knights != 0) {
            int knight = Bitwise.getNextBit(knights);

            int square = white ? knight ^ 56 : knight;
            mgScore += knightMgTable[square];
            egScore += knightEgTable[square];

            long attacks = Attacks.knightAttacks(knight);
            long moves = attacks &~ friendlyBlockers;
            int moveCount = Bitwise.countBits(moves);
            mgScore += knightMgMobility[moveCount];
            egScore += knightEgMobility[moveCount];

            long kingAttackZone = white ? blackKingSafetyZone : whiteKingSafetyZone;
            int attackZoneAttacks = Bitwise.countBits(kingAttackZone & attacks);
            addAttackZoneScore(MINOR_PIECE_ATTACK_UNIT, attackZoneAttacks, white);

            knights = Bitwise.popBit(knights);
        }

        addScore(mgScore, egScore, white);

    }

    /**
     * Bishop evaluation consists of simple piece-placement and mobility bonuses.
     */
    private void scoreBishops(long bishops, long friendlyBlockers, long opponentBlockers, boolean white) {

        if (bishops == 0) return;
        int mgScore = 0;
        int egScore = 0;

        long blockers = friendlyBlockers | opponentBlockers;
        while (bishops != 0) {
            int bishop = Bitwise.getNextBit(bishops);

            int square = white ? bishop ^ 56 : bishop;
            mgScore += bishopMgTable[square];
            egScore += bishopEgTable[square];

            long attacks = Attacks.bishopAttacks(bishop, blockers);
            long moves = attacks &~ friendlyBlockers;
            int moveCount = Bitwise.countBits(moves);
            mgScore += bishopMgMobility[moveCount];
            egScore += bishopEgMobility[moveCount];

            long kingAttackZone = white ? blackKingSafetyZone : whiteKingSafetyZone;
            int attackZoneAttacks = Bitwise.countBits(kingAttackZone & attacks);
            addAttackZoneScore(MINOR_PIECE_ATTACK_UNIT, attackZoneAttacks, white);

            bishops = Bitwise.popBit(bishops);
        }

        addScore(mgScore, egScore, white);

    }

    /**
     * Rook evaluation consists of piece-placement and mobility bonuses, as well as bonuses for being on an open- or semi-open file.
     */
    private void scoreRooks(long rooks,
                            long friendlyPawns,
                            long opponentPawns,
                            long friendlyBlockers,
                            long opponentBlockers,
                            boolean white) {

        if (rooks == 0) return;
        int mgScore = 0;
        int egScore = 0;

        long blockers = friendlyBlockers | opponentBlockers;
        while (rooks != 0) {
            int rook = Bitwise.getNextBit(rooks);
            int file = BoardUtils.getFile(rook);
            long fileMask = Bits.FILE_MASKS[file];

            int square = white ? rook ^ 56 : rook;
            mgScore += rookMgTable[square];
            egScore += rookEgTable[square];

            long attacks = Attacks.rookAttacks(rook, blockers);
            long moves = attacks &~ friendlyBlockers;
            int moveCount = Bitwise.countBits(moves);
            mgScore += rookMgMobility[moveCount];
            egScore += rookEgMobility[moveCount];

            long kingAttackZone = white ? blackKingSafetyZone : whiteKingSafetyZone;
            int attackZoneAttacks = Bitwise.countBits(kingAttackZone & attacks);
            addAttackZoneScore(ROOK_ATTACK_UNIT, attackZoneAttacks, white);

            boolean hasFriendlyPawn = (fileMask & friendlyPawns) != 0;
            boolean hasOpponentPawn = (fileMask & opponentPawns) != 0;
            boolean hasOpenFile = !hasFriendlyPawn && !hasOpponentPawn;
            boolean hasSemiOpenFile = !hasFriendlyPawn && hasOpponentPawn;

            if (hasOpenFile) {
                mgScore += config.getRookOpenFileBonus()[0];
                egScore += config.getRookOpenFileBonus()[1];
            }
            else if (hasSemiOpenFile) {
                mgScore += config.getRookSemiOpenFileBonus()[0];
                egScore += config.getRookSemiOpenFileBonus()[1];
            }

            rooks = Bitwise.popBit(rooks);
        }

        addScore(mgScore, egScore, white);
    }

    /**
     * Queen evaluation consists of simple piece-placement and mobility bonuses.
     */
    private void scoreQueens(long queens,
                             long friendlyBlockers,
                             long opponentBlockers,
                             boolean white) {

        if (queens == 0) return;
        int mgScore = 0;
        int egScore = 0;

        long blockers = friendlyBlockers | opponentBlockers;
        while (queens != 0) {
            int queen = Bitwise.getNextBit(queens);

            int square = white ? queen ^ 56 : queen;
            mgScore += queenMgTable[square];
            egScore += queenEgTable[square];

            long attacks = Attacks.bishopAttacks(queen, blockers) | Attacks.rookAttacks(queen, blockers);
            long moves = attacks &~ friendlyBlockers;
            int moveCount = Bitwise.countBits(moves);
            mgScore += queenMgMobility[moveCount];
            egScore += queenEgMobility[moveCount];

            long kingAttackZone = white ? blackKingSafetyZone : whiteKingSafetyZone;
            int attackZoneAttacks = Bitwise.countBits(kingAttackZone & attacks);
            addAttackZoneScore(QUEEN_ATTACK_UNIT, attackZoneAttacks, white);

            queens = Bitwise.popBit(queens);
        }

        addScore(mgScore, egScore, white);
    }

    /**
     * King evaluation consists of a piece-placement bonus, king safety considerations and mop-up bonus if up material.
     */
    private void scoreKing(long friendlyKing,
                           long friendlyPawns,
                           long opponentPawns,
                           Material opponentMaterial,
                           long friendlyBlockers,
                           long opponentBlockers,
                           Board board,
                           float phase,
                           boolean white) {
        int mgScore = 0;
        int egScore = 0;

        int king = Bitwise.getNextBit(friendlyKing);
        int square = white ? king ^ 56 : king;
        mgScore += kingMgTable[square];
        egScore += kingEgTable[square];
        addScore(mgScore, egScore, white);

        scoreKingSafety(king, friendlyPawns, opponentPawns, opponentMaterial, friendlyBlockers, opponentBlockers, board, phase, white);

    }

    /**
     * The king is penalised for being uncastled; for being castled but with the pawns infront of the king (the 'pawn shield')
     * pushed too far up the board; or for having open or semi-open files either infront of or adjacent to the king. This
     * evaluation matters less in the endgame, when there are fewer pieces on the board, and the king becomes a more active piece.
     * </p>
     * @see <a href="https://www.chessprogramming.org/King_Safety">Chess Programming Wiki</a>
     */
    private void scoreKingSafety(int kingSquare,
                                   long friendlyPawns,
                                   long opponentPawns,
                                   Material opponentMaterial,
                                   long friendlyBlockers,
                                   long opponentBlockers,
                                   Board board,
                                   float phase,
                                   boolean white) {
        // King safety evaluation
        int kingFile = BoardUtils.getFile(kingSquare);
        int pawnShieldPenalty = calculatePawnShieldPenalty(kingSquare, kingFile, friendlyPawns);
        int openKingFilePenalty = calculateOpenKingFilePenalty(kingFile, friendlyPawns, opponentPawns, opponentMaterial);
        int lostCastlingRightsPenalty = calculateLostCastlingRightsPenalty(config, board, white, kingFile);
        if (opponentMaterial.queens() == 0) {
            phase *= 0.4f;
        }
        int kingAttackZoneUnits = white ? whiteKingAttackZoneUnits : blackKingAttackZoneUnits;
        int attackZoneIndex = Math.min(kingAttackZoneUnits, config.getKingAttackZonePenaltyTable().length - 1);
        int attackZoneScore = config.getKingAttackZonePenaltyTable()[attackZoneIndex];

        int virtualMobilityMgPenalty = 0;
        int virtualMobilityEgPenalty = 0;
        long blockers = friendlyBlockers | opponentBlockers;
        long attacks = Attacks.bishopAttacks(kingSquare, blockers) | Attacks.rookAttacks(kingSquare, blockers);
        int moveCount = Bitwise.countBits(attacks);
        virtualMobilityMgPenalty += config.getVirtualKingMobilityPenalty()[0][moveCount];
        virtualMobilityEgPenalty += config.getVirtualKingMobilityPenalty()[1][moveCount];
        addScore(virtualMobilityMgPenalty, virtualMobilityEgPenalty, white);

        float kingSafetyScore = (int) -((pawnShieldPenalty + openKingFilePenalty + lostCastlingRightsPenalty + attackZoneScore) * phase);
        int mgScore = (int) ((kingSafetyScore / 100) * config.getKingSafetyScaleFactor()[0]);
        int egScore = (int) ((kingSafetyScore / 100) * config.getKingSafetyScaleFactor()[1]);
        addScore(mgScore, egScore, white);
    }

    private int calculatePawnShieldPenalty(int kingSquare, int kingFile, long pawns) {
        boolean isCastled = kingFile <= 2 || kingFile >= 5;
        if (!isCastled) return 0;
        int pawnShieldPenalty = 0;
        long pawnShield = Bitwise.getPawnShield(kingFile, pawns);
        while (pawnShield != 0) {
            int distance = Distance.chebyshev(kingSquare, Bitwise.getNextBit(pawnShield));
            pawnShieldPenalty += config.getKingPawnShieldPenalty()[distance];
            pawnShield = Bitwise.popBit(pawnShield);
        }
        return pawnShieldPenalty;
    }

    private int calculateOpenKingFilePenalty(int kingFile, long friendlyPawns, long opponentPawns, Material opponentMaterial) {
        if (opponentMaterial.rooks() == 0 && opponentMaterial.queens() == 0) {
            return 0;
        }
        return scoreFile(Bits.FILE_MASKS[kingFile], friendlyPawns, opponentPawns, true)
            + scoreFile(Bits.WEST_FILE_MASK[kingFile], friendlyPawns, opponentPawns, false)
            + scoreFile(Bits.EAST_FILE_MASK[kingFile], friendlyPawns, opponentPawns, false);
    }

    private int scoreFile(long fileMask, long friendlyPawns, long opponentPawns, boolean isKingFile) {
        if (fileMask == 0) {
            return 0;
        }
        int penalty = 0;
        boolean isFriendlyPawnMissing = (friendlyPawns & fileMask) == 0;
        boolean isOpponentPawnMissing = (opponentPawns & fileMask) == 0;
        if (isFriendlyPawnMissing || isOpponentPawnMissing) {
            // Add penalty for semi-open file around the king
            penalty += isKingFile ? config.getKingSemiOpenFilePenalty() : config.getKingSemiOpenAdjacentFilePenalty();
        }
        if (isFriendlyPawnMissing && isOpponentPawnMissing) {
            // Add penalty for fully open file around king
            penalty += isKingFile ? config.getKingOpenFilePenalty() : config.getKingOpenAdjacentFilePenalty();
        }
        return penalty;
    }

    private int calculateLostCastlingRightsPenalty(EngineConfig config, Board board, boolean white, int kingFile) {
        boolean isCastled = kingFile <= 2 || kingFile >= 5;
        if (isCastled) return 0;
        boolean hasCastlingRights = board.getGameState().hasCastlingRights(white);
        boolean opponentHasCastlingRights = board.getGameState().hasCastlingRights(!white);
        return !hasCastlingRights && opponentHasCastlingRights ? config.getKingLostCastlingRightsPenalty() : 0;
    }

    private int mgMaterialScore(Material material) {
        return (material.pawns() * pawnMgValue) +
               (material.knights() * knightMgValue) +
               (material.bishops() * bishopMgValue) +
               (material.rooks() * rookMgValue) +
               (material.queens() * queenMgValue) +
               (material.bishops() == 2 ? config.getBishopPairBonus() : 0);
    }

    private int egMaterialScore(Material material) {
        return (material.pawns() * pawnEgValue) +
                (material.knights() * knightEgValue) +
                (material.bishops() * bishopEgValue) +
                (material.rooks() * rookEgValue) +
                (material.queens() * queenEgValue) +
                (material.bishops() == 2 ? config.getBishopPairBonus() : 0);
    }

    public void addScore(int middlegameScore, int endgameScore, boolean white) {
        if (white) {
            whiteMgScore += middlegameScore;
            whiteEgScore += endgameScore;
        } else {
            blackMgScore += middlegameScore;
            blackEgScore += endgameScore;
        }
    }

    private void addAttackZoneScore(int attackUnit, int attackCount, boolean white) {
        int attackZoneScore = attackUnit * attackCount;
        if (white) blackKingAttackZoneUnits += attackZoneScore;
        else       whiteKingAttackZoneUnits += attackZoneScore;

    }

    public int sum(boolean white) {
        int whiteScore = Phase.taperedEval(whiteMgScore, whiteEgScore, phase);
        int blackScore = Phase.taperedEval(blackMgScore, blackEgScore, phase);
        int score = whiteScore - blackScore;
        int modifier = white ? 1 : -1;
        return score * modifier;
    }

    @Override
    public void clearHistory() {
        pawnHash.clear();
    }

    @Override
    public void setPosition(Board board) {

    }

}
