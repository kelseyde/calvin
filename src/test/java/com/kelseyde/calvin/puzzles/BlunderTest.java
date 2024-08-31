package com.kelseyde.calvin.puzzles;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.Engine;
import com.kelseyde.calvin.uci.UCI;
import com.kelseyde.calvin.uci.UCICommand.PositionCommand;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * These are all positions from Calvin's games on Lichess, where he commits a horrific blunder.
 */
@Disabled
public class BlunderTest {

    private final Engine engine = UCI.ENGINE;

    @Test
    public void testDontSacKnightForCenterPawn() {

        String fen = "r1bqkb1r/1pp1pppp/p1n2n2/8/2BPP3/2N2N2/PP3PPP/R1BQK2R b KQkq - 0 6";
        engine.newGame();
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        engine.setThreadCount(1);
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("f6e4")));

    }

    @Test
    public void testDontSacKnightForCenterPawn2() {

        String fen = "rnbqk2r/ppp2ppp/3b1n2/4p3/3pP3/5N2/PPPPNPPP/1RBQKB1R w Kkq - 4 6";
        engine.newGame();
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("f3d4")));
        Assertions.assertFalse(move.matches(Notation.fromUCI("e2d4")));

    }

    @Test
    public void testDontMoveRookBeforeCastling() {

        String fen = "r1b1kbnr/ppp2ppp/2n1p3/3q4/3P4/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 5";

        engine.newGame();
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("h1g1")));

    }

    @Test
    public void testDontSacYourQueenForPawn() {

        String fen = "rnbq1rk1/ppp2ppp/5n2/4p3/2P5/3P2P1/PQ2PP1P/R1B1KBNR b KQ - 2 9";

        engine.newGame();
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("d8d3")));

    }

    @Test
    public void testAnotherKnightSac() {

        String fen = "r2qkb1r/ppp1pppp/2n2n2/3p4/3PP3/2N2P1P/PPP2P2/R1BQKB1R b KQkq - 0 6";

        engine.newGame();
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("f6e4")));

    }

    @Test
    public void testDontRepeatWhenCompletelyWinning() {

        String fen = "7r/4b1p1/8/3BkP2/4N3/8/PPn2PP1/1R1R2K1 b - - 0 26";
        engine.newGame();
        List<Move> moves = new ArrayList<>();
        moves.add(Notation.fromNotation("h8", "b8"));
        moves.add(Notation.fromNotation("e4", "c3"));
        moves.add(Notation.fromNotation("e7", "c5"));
        moves.add(Notation.fromNotation("c3", "e4"));
        moves.add(Notation.fromNotation("c5", "e7"));
        moves.add(Notation.fromNotation("e4", "c3"));
        moves.add(Notation.fromNotation("e7", "c5"));
        engine.setPosition(new PositionCommand(fen, moves));

        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("c3e4")));

    }

    @Test
    public void testDontPushPawnShield() {

        String fen = "r1bq2k1/ppp1nppp/5b2/3pN3/3P1B2/2PB4/P1P2PPP/1R2Q1K1 b - - 2 14";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("g7g5")));
    }

    @Test
    public void testDontTrapOwnQueen() {

        String fen = "r1b2rk1/ppp2ppp/2n1pn2/q5N1/2PP4/P2B1N2/1P1Q1PPP/R3K2R b KQ - 0 11";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));

        List<Move> goodMoves = List.of(
                Notation.fromUCI("a5d2"),
                Notation.fromUCI("h7h6"),
                Notation.fromUCI("a5a6"),
                Notation.fromUCI("a5b6"),
                Notation.fromUCI("a7a6"));

        Assertions.assertTrue(goodMoves.stream().anyMatch(move::equals));

    }

    @Test
    public void testDontSacQueenForKnight() {

        String fen = "rnb1kb1r/ppN1pppp/5n2/8/P1qppB2/2P5/4NPPP/R2QK2R b KQkq - 1 11";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(move.matches(Notation.fromUCI("c4c7")));

    }

    @Test
    public void testIgnoreQueenKnightMatingAttack() {

        String fen = "r4rk1/pp2ppb1/3p1npp/4n3/4P3/1BN1BP1q/PPP2P1P/R2Q1R1K w - - 5 15";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(move.matches(Notation.fromUCI("f1g1")));

    }

    @Test
    public void testDontBlunderRook() {

        String fen = "r7/p5kp/1p3np1/2n1B3/2P1B3/5P1P/Pr3P2/3R2K1 b - - 0 31";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("c5e4"))
            ||  move.matches(Notation.fromUCI("e2e4"))
        );

    }

    @Test
    public void testDontBlunderRook2() {

        String fen = "1rb3k1/p1q3pp/4pr2/5p2/2pP4/1PQ3P1/4PPBP/2R1K2R b K - 0 21";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("b8b3"))
        );

    }

    @Test
    public void testDontBlunderQueen() {

        String fen = "2b3k1/p5pp/4pr2/q4p2/3P4/2Q3P1/4PPBP/2R1K2R b K - 2 25";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("a5c7"))
        );

    }

    @Test
    public void testDontBlunderQueenDiscoveredCheck() {

        String fen = "3r1rk1/1p4pp/1p6/1P2p1p1/P4nP1/2BnK1RP/2BQ1P1q/5R2 w - - 0 37";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("c2d3"))
        );

    }

    @Test
    public void testDontBlunderQueen2() {

        String fen = "r3kbnr/pp1qpp1p/2np2p1/8/3pP3/2N2N2/PPP2PPP/R1BQ1RK1 w kq - 0 8";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("d1d4")));

    }

    @Test
    public void testDontWalkIntoPin() {

        String fen = "r1b1kb1r/pppp1ppp/2n5/2q1P1B1/2Bp2n1/5N2/PPPNQPPP/R3K2R b KQkq - 6 8";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("g4e5")));

    }

    @Test
    public void testDontBlunderBishop() {

        String fen = "r3kbnr/pp3ppp/2p5/8/4P1b1/5P2/PP2BP1P/RNBK3R b kq - 0 10";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("g4f3")));

    }

    @Test
    public void testDontSacBishopForTwoPawns() {

        String fen = "r2q1rk1/1pp2ppp/p1b5/3P4/n1P5/4QN2/P2N1PPP/1R3RK1 b - - 0 19";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("c6d5")));

    }

    @Test
    public void testDontBlunderRook3() {

        String fen = "3r4/4kp2/q1p2p1b/p1p1pP2/2P3Qp/1R2N2P/PP1r1PPK/1R6 w - - 4 41";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("b3b6"))
        );

    }

    @Test
    public void testDontMoveKingBeforeCastling() {

        String fen = "r1bqk2r/ppp2ppp/2n1pb2/8/3P4/2PB1N2/PP3PPP/R1BQ1RK1 b kq - 3 9";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("e8f8"))
        );

    }

    @Test
    public void testDontMoveKingBeforeCastling2() {

        String fen = "rn2kb1r/pp4pp/2p1p3/3n4/1qNP1p2/2N5/PPP1Q1PP/R1B2RK1 b kq - 5 14";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("e8d8"))
        );

    }

    @Test
    public void testDontSacExchangeForSpeculativeCompensation() {

        String fen = "4r1k1/5pp1/7p/p3Bq2/Prb2P2/R1Q5/1P2p1PP/4R2K b - - 1 33";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("e8e5"))
        );

    }

    @Test
    public void testCastleOutOfDanger() {

        String fen = "r3k2r/pppqppb1/4b2p/4P2Q/3pP3/2N4P/PPP1BP1P/2KR3R b kq - 2 14";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("a8d8"))
        );

    }

    @Test
    public void testDontSacExchangeForSpeculativeCompensation2() {

        String fen = "5r2/kb4p1/p1p1p1qp/3pP3/3Pr3/1Q3NPP/PPR3K1/5R2 b - - 6 29";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("f8f3"))
        );

    }

    @Test
    public void testDontLetOpponentBuildBigCenter() {

        String fen = "4kb1r/3r1ppp/1q2p3/p2p1b2/2PP4/1P2P3/1B1NQPPP/R4RK1 b k - 0 17";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(500).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("d5c4"))
        );

    }

    @Test
    public void testDontLosePawnInDrawnEndgame() {

        String fen = "8/8/4k3/6p1/1R2Bb1p/5P1P/1p1r1P2/4K3 b - - 9 57";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("g5g4"))
        );

    }

    @Test
    public void testOneMoveToPreventMateThreat() {

        String fen = "4Q3/5p1k/5Pp1/5bP1/p7/3p1Nb1/q2B4/6K1 b - - 5 65";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(200).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("g3d6"))
        );

    }

    @Test
    public void testDontSacExchangeAgain() {

        String fen = "5r2/kb4p1/p1p1p1qp/3pP3/3Pr3/1Q3NPP/PPR3K1/5R2 b - - 6 29";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("f8f3"))
        );

    }

    @Test
    public void testAvoidForcedDraw() {

        String fen = "R7/2p1k2p/1r1pBpqb/3P4/1P2bP2/2P3P1/1B5K/4Q3 w - - 5 51";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("h2g1"))
        );

    }

    @Test
    public void testDontSacBishopOnF3() {

        String fen = "7k/1b2Bp1p/5p2/p3pq2/1pP5/1r3P2/1P3QPP/4R1K1 b - - 1 33";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("b7f3"))
        );

    }

    @Test
    public void testDontMoveBishopWhereItCanBeKicked() {

        String fen = "r1b1kb1r/ppp2ppp/2n2n2/q3p3/4P3/P1NP1N2/1P3PPP/R1BQKB1R b KQkq - 2 7";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("f8c5"))
        );

    }

    @Test
    public void testDontOpenHFileToKing() {

        String fen = "4rrk1/p4pp1/1p5p/5PP1/2P5/P1PQpq2/4R1R1/2K5 b - - 0 35";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("h6h5"))
        );

    }

    @Test
    public void testDefendAgainstHeavyPieceMatingAttack() {

        String fen = "4r1k1/p4pp1/1p6/5P2/2P1rq2/P1PQp3/4R3/1K5R b - - 6 39";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("f4f5"))
        );

    }

    @Test
    public void testDontSacBishopTacticWhenKingIsInDanger() {

        String fen = "3r2k1/5rp1/p1R3N1/1p2b2P/6Q1/P2q4/5PP1/5RK1 b - - 4 34";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("e5h2"))
        );

    }

    @Test
    public void testDontLetOpponentBuildQueenBishopBattery() {

        String fen = "5rk1/2q2pp1/b4b2/rpp2Q2/2P3P1/1B2PN1P/P5K1/3R1R2 b - - 2 25";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("c7c8")) ||
                        move.matches(Notation.fromUCI("a6a8"))
        );

    }

    @Test
    public void testDontBlockBishopAttackOnRookWithFreePawn() {

        String fen = "r1bq1rk1/2pn1pp1/p3pb1p/8/Pp1PB3/2N1PN2/1P3PPP/R2QK2R b KQ - 1 13";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("c7c6"))
        );
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("a8b8")) ||
                        move.matches(Notation.fromUCI("a8a7"))
        );

    }

    @Test
    public void testDontHangKnightForTwoPawns() {

        String fen = "3rrqk1/2pp1pp1/p6p/4P2Q/Bp2nP2/2P5/PP2R1PP/3R2K1 b - - 0 22";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("e4c5"))
        );

    }

    @Test
    public void testDontThrowDrawnOppositeBishopsEndgame() {

        String fen = "8/8/1kp2P2/1p3K2/p1B5/P1P5/1b6/8 w - - 0 53";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("f6f7"))
        );

    }

    @Test
    public void testDontAllowDesperadoOnF7() {

        String fen = "rnbqkb1r/pppp1ppp/8/4N3/4n3/8/PPPPQPPP/RNB1KB1R b KQkq - 1 4";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(4000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("d8e7"))
        );

    }

    @Test
    public void testDontAllowBishopSacOnH6() {

        String fen = "2rqbrk1/1p2bppp/4pn2/p3N3/1n1P1B2/2N4Q/PP3PPP/1BRR2K1 b - - 1 17";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(4000).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("h7h6"))
        );

    }

    @Test
    public void testDontBlunderKnight() {

        String fen = "3rn3/1p3bpk/2nB3p/p1p1P3/8/1PP2P1P/P2R2P1/4R1K1 b - - 2 32";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1360).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("c6e5"))
        );

    }

    @Test
    public void testDontSacKnightForPawn() {

        String fen = "3r2k1/1p4p1/p2q3p/3P1p2/2QNp1b1/P5P1/1P3PPK/4R3 w - - 4 27";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1360).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("d4c6"))
        );

    }

    @Test
    public void testDontMissCrushingAttack() {

        String fen = "3rk3/bp4p1/4p1p1/N3Pp2/5P1P/PQ4P1/1P1pqPK1/3R4 b - - 5 31";
        engine.setPosition(new PositionCommand(fen, Collections.emptyList()));
        Move move = engine.think(1360).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertTrue(
                move.matches(Notation.fromUCI("e2f2"))
        );

    }

    @Test
    public void testDontForceRepetitionWhenMating() {

        String fen = "4K3/5p2/4q2p/8/4kb2/8/8/8 w - - 10 81";
        Board board = FEN.toBoard(fen);
        board.makeMove(Notation.fromUCI("e8d8"));
        board.makeMove(Notation.fromUCI("e6e5"));
        board.makeMove(Notation.fromUCI("d8d7"));
        board.makeMove(Notation.fromUCI("e5e6"));
        board.makeMove(Notation.fromUCI("d7d8"));
        board.makeMove(Notation.fromUCI("e6e5"));
        board.makeMove(Notation.fromUCI("d8d7"));

        engine.setPosition(board);

        Move move = engine.think(200).move();
        System.out.println(Notation.toNotation(move));
        Assertions.assertFalse(
                move.matches(Notation.fromUCI("e5e6"))
        );

    }

    @Test
    public void testStalemate2() {

        String fen = "2R5/8/p7/7p/6pP/5pP1/5P1K/k4q2 w - - 0 1";
        Board board = FEN.toBoard(fen);

        engine.setThreadCount(1);
        engine.setPosition(board);

        int eval = engine.think(1000).eval();
        Assertions.assertEquals(0, eval);

    }

    @Test
    public void testConnectionStalls() {

        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - \n";
        Board board = FEN.toBoard(fen);
        engine.setThreadCount(1);
        engine.setPosition(board);
        Move move = engine.think(500).move();
        System.out.println(move);

    }

}
