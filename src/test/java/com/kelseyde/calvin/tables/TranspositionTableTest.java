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
        table = new TranspositionTable(TestUtils.CONFIG.defaultHashSizeMb);
    }

    @Test
    public void testBasicEntry() {

        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 17;
        int score = 548;
        int flag = HashFlag.EXACT;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);

    }

    @Test
    public void testNullMoveEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = 1;
        int flag = HashFlag.UPPER;
        assertEntry(zobristKey, score, null, flag, depth);
    }

    @Test
    public void testCheckmateEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = 1000000;
        int flag = HashFlag.UPPER;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testNegativeCheckmateEntry() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 1;
        int score = -1000000;
        int flag = HashFlag.UPPER;
        Move move = Move.fromUCI("e2e4");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testMaxDepth() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        int flag = HashFlag.UPPER;
        assertEntry(zobristKey, score, null, flag, depth);
    }

    @Test
    public void testPromotionFlag() {
        Board board = FEN.toBoard("3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23");
        long zobristKey = board.getState().getKey();
        int depth = 256;
        int score = -789;
        int flag = HashFlag.LOWER;
        Move move = Move.fromUCI("e7e8q");
        assertEntry(zobristKey, score, move, flag, depth);
    }

    @Test
    public void testSimplePutAndGetExact() {

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));

        // Do some evaluation on the node at this position.
        int flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e2e4");
        int eval = 60;
        int depth = 3;
        int ply = 2;

        table.put(board.getState().getKey(), flag, depth, ply, bestMove,  0, eval);

        // Do some more searching, return to this position

        HashEntry entry = table.get(board.getState().getKey(), ply);

        Assertions.assertNotNull(entry);
        Assertions.assertEquals(flag, entry.flag());
        Assertions.assertEquals(bestMove, entry.move());
        Assertions.assertEquals(eval, entry.score());
        Assertions.assertEquals(depth, entry.depth());

        board.makeMove(TestUtils.getLegalMove(board, "d2", "d4"));
        flag = HashFlag.UPPER;
        bestMove = Move.fromUCI("e2e4");
        eval = 28666;
        depth = 256;
        table.put(board.getState().getKey(), flag, depth, ply + 1, bestMove, 0,  eval);

        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals(flag, entry.flag());
        Assertions.assertEquals(bestMove, entry.move());
        Assertions.assertEquals(eval, entry.score());
        Assertions.assertEquals(depth, entry.depth());

        board.makeMove(TestUtils.getLegalMove(board, "g8", "f6"));
        flag = HashFlag.LOWER;
        eval = 1000000;
        depth = 10;
        table.put(board.getState().getKey(), flag, depth, ply + 2, null, 0,  eval);

        entry = table.get(board.getState().getKey(), ply);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals(flag, entry.flag());
        Assertions.assertEquals(null, entry.move());
        Assertions.assertEquals(eval - 2, entry.score());
        Assertions.assertEquals(depth, entry.depth());
    }

    @Test
    public void testSimplePutAndGetNotFound() {

        board.makeMove(TestUtils.getLegalMove(board, "e2", "e4"));
        board.makeMove(TestUtils.getLegalMove(board, "e7", "e5"));

        // Do some evaluation on the node at this position.
        int flag = HashFlag.EXACT;
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

        int flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int eval = 60;
        int depth = 3;
        int ply = 255;

        table.put(board.getState().getKey(), flag, depth, ply, bestMove, 0,  eval);

        // Do some more searching, return to this position

        HashEntry entry = table.get(board.getState().getKey(), ply);

        Assertions.assertNotNull(entry);
        Assertions.assertEquals(flag, entry.flag());
        Assertions.assertEquals(bestMove, entry.move());
        Assertions.assertTrue(entry.move() != null && entry.move().isPromotion());
        Assertions.assertEquals(Piece.BISHOP, entry.move().promoPiece());
        Assertions.assertEquals(eval, entry.score());
        Assertions.assertEquals(depth, entry.depth());

    }

    @Test
    public void testStoreCheckmateAtRoot() {

        int flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int plyRemaining = 10;
        int plyFromRoot = 0;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  1000000);

        Assertions.assertEquals(1000000, table.get(board.getState().getKey(), 0).score());

        table.put(board.getState().getKey(), flag, plyRemaining + 1, plyFromRoot, bestMove, 0,  -1000000);

        Assertions.assertEquals(-1000000, table.get(board.getState().getKey(), 0).score());

    }

    @Test
    public void testStoreCheckmateAtRootPlusOne() {

        int flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int plyRemaining = 10;
        int plyFromRoot = 1;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  1000000);

        Assertions.assertEquals(999999, table.get(board.getState().getKey(), 0).score());

        table.put(board.getState().getKey(), flag, plyRemaining + 1, plyFromRoot, bestMove, 0,  -1000000);

        Assertions.assertEquals(-999999, table.get(board.getState().getKey(), 0).score());

    }

    @Test
    public void testScorePositiveCheckmateThenAccessFromDeeperPly() {

        long zobrist = board.getState().getKey();
        int flag = HashFlag.EXACT;
        Move bestMove = Move.fromUCI("e7e8b");
        int eval = 1000000;
        int plyRemaining = 10;
        int plyFromRoot = 5;

        table.put(board.getState().getKey(), flag, plyRemaining, plyFromRoot, bestMove, 0,  eval);

        Assertions.assertEquals(1000000, table.get(zobrist, 5).score());
        Assertions.assertEquals(999999, table.get(zobrist, 4).score());
        Assertions.assertEquals(999998, table.get(zobrist, 3).score());
        Assertions.assertEquals(999997, table.get(zobrist, 2).score());
        Assertions.assertEquals(999996, table.get(zobrist, 1).score());
        Assertions.assertEquals(999995, table.get(zobrist, 0).score());
    }

    @Test
    public void testDoesNotReplaceEntryWithMoreDepth() {

        long zobrist = board.getState().getKey();
        int flag = HashFlag.EXACT;
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
        int flag = HashFlag.EXACT;
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

    @Test
    public void testStoreOnlyStaticEvalInTt() {

        long key = board.key();
        int flag = HashFlag.NONE;
        int depth = 0;
        int ply = 0;
        int eval = 126;
        int score = 0;

        table.put(key, flag, depth, ply, null, eval, score);

        HashEntry ttEntry = table.get(key, ply);

        Assertions.assertNotNull(ttEntry);
        Assertions.assertEquals(HashFlag.NONE, ttEntry.flag());
        Assertions.assertNull(ttEntry.move());
        Assertions.assertEquals(126, ttEntry.staticEval());
        Assertions.assertEquals(0, ttEntry.score());
        Assertions.assertEquals(0, ttEntry.depth());

    }

    private void assertEntry(long zobrist, int score, Move move, int flag, int depth) {
        long key = HashEntry.Key.of(zobrist, 0, 0);
        long value = HashEntry.Value.of(score, move, flag, depth);
        HashEntry entry = HashEntry.of(key, value);
        Assertions.assertEquals(depth, entry.depth());
        Assertions.assertEquals(score, entry.score());
        Assertions.assertEquals(flag, entry.flag());
        Assertions.assertEquals(move, entry.move());
    }

}