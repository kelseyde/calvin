package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.utils.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class MovePickerTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final MoveOrderer moveOrderer = new MoveOrderer();

    @Test
    public void testInCheckDoesNotGenerateMovesTwice() {

        String fen = "rnbqkbnr/1p2pppp/p2p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 1";
        Board board = FEN.toBoard(fen);

        MovePicker picker = new MovePicker(moveGenerator, moveOrderer, board, new SearchStack(), 0);
        picker.setInCheck(true);

        List<Move> moves = new ArrayList<>();
        while (true) {
            Move move = picker.pickNextMove();
            if (move == null) break;
            moves.add(move);
        }

        Assertions.assertEquals(5, moves.size());
    }

}