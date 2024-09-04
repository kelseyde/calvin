package com.kelseyde.calvin.tables;

import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.tables.history.ContHistTable;
import com.kelseyde.calvin.utils.Notation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContHistTableTest {

    @Test
    public void testNoMatch() {
        ContHistTable contHistTable = new ContHistTable();
        Move prevMove = Notation.fromUCI("e2e4");
        Piece prevPiece = Piece.PAWN;
        Move currMove = Notation.fromUCI("d5e4");
        Piece currPiece = Piece.PAWN;
        assertEquals(0, contHistTable.get(prevMove, prevPiece, currMove, currPiece, true));
    }

    @Test
    public void testAdd() {
        ContHistTable contHistTable = new ContHistTable();
        Move prevMove = Notation.fromUCI("e2e4");
        Piece prevPiece = Piece.PAWN;
        int depth = 8;
        Move currMove = Notation.fromUCI("d5e4");
        Piece currPiece = Piece.PAWN;
        contHistTable.add(prevMove, prevPiece, currMove, currPiece, depth, true);
        assertEquals(1200, contHistTable.get(prevMove, prevPiece, currMove, currPiece, true));
    }

    @Test
    public void testSub() {
        ContHistTable contHistTable = new ContHistTable();
        Move prevMove = Notation.fromUCI("e2e4");
        Piece prevPiece = Piece.PAWN;
        int depth = 8;
        Move currMove = Notation.fromUCI("d5e4");
        Piece currPiece = Piece.PAWN;
        contHistTable.sub(prevMove, prevPiece, currMove, currPiece, depth, true);
        assertEquals(-1200, contHistTable.get(prevMove, prevPiece, currMove, currPiece, true));
    }

    @Test
    public void testAddWithSearchStack() {
        ContHistTable contHistTable = new ContHistTable();
        SearchStack ss = new SearchStack();
        Move prevMove = Notation.fromUCI("e2e4");
        Piece prevPiece = Piece.PAWN;
        ss.setMove(0, prevMove, prevPiece);
        int depth = 8;
        Move currMove = Notation.fromUCI("d5e4");
        Piece currPiece = Piece.PAWN;
        contHistTable.add(ss.getMove(0), ss.getMovedPiece(0), currMove, currPiece, depth, true);
        assertEquals(1200, contHistTable.get(ss.getMove(0), ss.getMovedPiece(0), currMove, currPiece, true));
    }

    @Test
    public void testAddTwice() {
        ContHistTable contHistTable = new ContHistTable();
        Move prevMove = Notation.fromUCI("e2e4");
        Piece prevPiece = Piece.PAWN;
        int depth = 8;
        Move currMove = Notation.fromUCI("d5e4");
        Piece currPiece = Piece.PAWN;
        contHistTable.add(prevMove, prevPiece, currMove, currPiece, depth, true);
        contHistTable.add(prevMove, prevPiece, currMove, currPiece, depth, true);
        assertEquals(2225, contHistTable.get(prevMove, prevPiece, currMove, currPiece, true));
    }

}