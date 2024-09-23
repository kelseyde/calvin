package com.kelseyde.calvin.tables;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.tables.tt.HashEntry;
import com.kelseyde.calvin.tables.tt.HashFlag;
import com.kelseyde.calvin.tables.tt.TranspositionTable;
import com.kelseyde.calvin.utils.TestUtils;
import com.kelseyde.calvin.utils.notation.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TranspositionTableTest {

    private TranspositionTable table;

    private Board board;

    @BeforeEach
    public void beforeEach() {
        board = Board.from(FEN.STARTPOS);
        table = new TranspositionTable(TestUtils.CONFIG.ttSizeDefault, TestUtils.CONFIG.ttBucketSize);
    }

    @Test
    public void testBasicEntry() {

        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 17;
        int score = 548;
        HashFlag flag = HashFlag.EXACT;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);

    }

    @Test
    public void testNullMoveEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = 1;
        HashFlag flag = HashFlag.UPPER;
        Move move = null;
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testCheckmateEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = 1000000;
        HashFlag flag = HashFlag.UPPER;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testNegativeCheckmateEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = -1000000;
        HashFlag flag = HashFlag.UPPER;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testMaxDepth() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.UPPER;
        Move move = null;
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testPromotionFlag() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8q");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testSetScore() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8n");
        HashEntry entry = HashEntry.of(zobristKey, score, 0, move, flag, depth, 0);

        Assertions.assertEquals(-789, entry.getScore());

        entry.setScore(43);
        Assertions.assertEquals(43, entry.getScore());
    }

    @Test
    public void testSetMove() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8n");
        HashEntry entry = HashEntry.of(zobristKey, score,  0, move, flag, depth, 0);

        Assertions.assertEquals(Move.fromUCI("e7e8n"), entry.getMove());

        entry.setMove(Move.fromUCI("e7e8", Move.EN_PASSANT_FLAG));
        Assertions.assertEquals(Move.fromUCI("e7e8", Move.EN_PASSANT_FLAG), entry.getMove());
    }

    @Test
    public void testSetGeneration() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8n");
        HashEntry entry = HashEntry.of(zobristKey, score, 0,  move, flag, depth, 0);

        Assertions.assertEquals(0, entry.getAge());

        entry.setAge(127);
        Assertions.assertEquals(127, entry.getAge());
    }

    @Test
    public void testSetStaticEval() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        HashFlag flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8n");
        int staticEval = 10;
        HashEntry entry = HashEntry.of(zobristKey, score, staticEval,  move, flag, depth, 0);

        Assertions.assertEquals(10, entry.getStaticEval());

        entry.setStaticEval(-4234);
        Assertions.assertEquals(-4234, entry.getStaticEval());
    }

    @Test
    public void testSimplePutAndGetExact() {

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));

        // Do some evaluation on the node at this position.
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e2e4");
        int eval = 60;
        int depth = 3;
        int ply = 2;

        table.put(board.getState().getKey(), flag, depth, ply, bestMove,  0, eval);

        // Do some more searching, return to this position

        HashEntry entry = table.get(board.getState().getKey(), ply);

        Assertions.assertNotNull(entry);
        Assertions.assertEquals(HashEntry.keyPart(board.getState().getKey()), entry.getKeyPart());
        Assertions.assertEquals(flag, entry.getFlag());
        Assertions.assertEquals(bestMove, entry.getMove());
        Assertions.assertEquals(eval, entry.getScore());
        Assertions.assertEquals(depth, entry.getDepth());

        board.makeMove(TestUtils.getLegalMove(board, "d2", "d4"));
        flag = HashFlag.UPPER;
        bestMove = Move.fromUCI("e2e4");
        eval = 28666;
        depth = 256;
        table.put(board.getState().getKey(), flag, depth, ply + 1, bestMove, 0,  eval);

        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals(HashEntry.keyPart(board.getState().getKey()), entry.getKeyPart());
        Assertions.assertEquals(flag, entry.getFlag());
        Assertions.assertEquals(bestMove, entry.getMove());
        Assertions.assertEquals(eval, entry.getScore());
        Assertions.assertEquals(depth, entry.getDepth());

        board.makeMove(TestUtils.getLegalMove(board, "g8", "f6"));
        flag = HashFlag.LOWER;
        bestMove = null;
        eval = 1000000;
        depth = 10;
        table.put(board.getState().getKey(), flag, depth, ply + 2, bestMove, 0,  eval);

        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals(HashEntry.keyPart(board.getState().getKey()), entry.getKeyPart());
        Assertions.assertEquals(flag, entry.getFlag());
        Assertions.assertEquals(bestMove, entry.getMove());
        Assertions.assertEquals(eval - 2, entry.getScore());
        Assertions.assertEquals(depth, entry.getDepth());
    }

    @Test
    public void testSimplePutAndGetNotFound() {

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));

        // Do some evaluation on the node at this position.
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e2e4");
        int eval = 60;
        int depth = 3;
        int ply = 25;

        table.put(board.getState().getKey(), flag, depth, ply, bestMove, 0,  eval);

        board.unmakeMove();
        board.unmakeMove();

        // Do some more searching, return to this position

        HashEntry entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNull(entry);

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNull(entry);

        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));
        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNotNull(entry);

    }

    @Test
    public void testCanStorePromotionFlag() {

        long zobrist = board.getState().getKey();
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int eval = 60;
        int depth = 3;
        int ply = 255;

        table.put(board.getState().getKey(), flag, depth, ply, bestMove, 0,  eval);

        // Do some more searching, return to this position

        HashEntry entry = table.get(board.getState().getKey(), ply);

        Assertions.assertNotNull(entry);
        Assertions.assertEquals(HashEntry.keyPart(zobrist), entry.getKeyPart());
        Assertions.assertEquals(flag, entry.getFlag());
        Assertions.assertEquals(bestMove, entry.getMove());
        Assertions.assertTrue(entry.getMove() != null && entry.getMove().isPromotion());
        Assertions.assertEquals(Piece.BISHOP, entry.getMove().promoPiece());
        Assertions.assertEquals(eval, entry.getScore());
        Assertions.assertEquals(depth, entry.getDepth());

    }

    @Test
    public void testStoreCheckmateAtRoot() {

        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int plyRemaining = 10;
        int plyFromRoot = 0;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  1000000);

        Assertions.assertEquals(1000000, table.get(board.getState().getKey(), 0).getScore());

        table.put(board.getState().getKey(), flag, plyRemaining + 1, plyFromRoot, bestMove, 0,  -1000000);

        Assertions.assertEquals(-1000000, table.get(board.getState().getKey(), 0).getScore());

    }

    @Test
    public void testStoreCheckmateAtRootPlusOne() {

        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int plyRemaining = 10;
        int plyFromRoot = 1;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  1000000);

        Assertions.assertEquals(999999, table.get(board.getState().getKey(), 0).getScore());

        table.put(board.getState().getKey(), flag, plyRemaining + 1, plyFromRoot, bestMove, 0,  -1000000);

        Assertions.assertEquals(-999999, table.get(board.getState().getKey(), 0).getScore());

    }

    @Test
    public void testScorePositiveCheckmateThenAccessFromDeeperPly() {

        long zobrist = board.getState().getKey();
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int eval = 1000000;
        int plyRemaining = 10;
        int plyFromRoot = 5;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        Assertions.assertEquals(1000000, table.get(zobrist, 5).getScore());
        Assertions.assertEquals(999999, table.get(zobrist, 4).getScore());
        Assertions.assertEquals(999998, table.get(zobrist, 3).getScore());
        Assertions.assertEquals(999997, table.get(zobrist, 2).getScore());
        Assertions.assertEquals(999996, table.get(zobrist, 1).getScore());
        Assertions.assertEquals(999995, table.get(zobrist, 0).getScore());
    }

    @Test
    public void testDoesNotReplaceEntryWithMoreDepth() {

        long zobrist = board.getState().getKey();
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e2e4");
        int eval = 60;
        int plyFromRoot = 0;
        int plyRemaining = 12;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        flag = HashFlag.UPPER;
        eval = 70;
        plyRemaining = 11;
        bestMove = Move.fromUCI("e2e4");
        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        assertEntry(zobrist, 60, Move.fromUCI("e2e4"), HashFlag.EXACT, 12);

    }

    @Test
    public void testReplacesEntryWithLessDepth() {

        long zobrist = board.getState().getKey();
        HashFlag flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e2e4");
        int eval = 60;
        int plyFromRoot = 0;
        int plyRemaining = 12;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        flag = HashFlag.UPPER;
        eval = 70;
        plyRemaining = 13;
        bestMove = Move.fromUCI("e2e4");
        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        assertEntry(zobrist, 60, bestMove, flag, 13);

    }

    private void assertEntry(long zobrist, int score, Move move, HashFlag flag, int depth) {
        HashEntry entry = HashEntry.of(zobrist, score, 0,  move, flag, depth, 0);
        Assertions.assertEquals(HashEntry.keyPart(zobrist), entry.getKeyPart());
        Assertions.assertEquals(depth, entry.getDepth());
        Assertions.assertEquals(score, entry.getScore());
        Assertions.assertEquals(flag, entry.getFlag());
        Assertions.assertEquals(move, entry.getMove());
    }

}