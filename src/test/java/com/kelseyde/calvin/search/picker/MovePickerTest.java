package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.generation.MoveGenerator;
import com.kelseyde.calvin.search.moveordering.MoveOrderer;
import com.kelseyde.calvin.utils.Bench;
import com.kelseyde.calvin.utils.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MovePickerTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final MoveOrderer moveOrderer = new MoveOrderer();

    @Test
    public void testInCheckDoesNotGenerateMovesTwice() {

        String fen = "rnbqkbnr/pp2pppp/3p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 1";
        Board board = FEN.toBoard(fen);

        MovePicker picker = new MovePicker(moveGenerator, moveOrderer, board, 0);

        List<Move> moves = new ArrayList<>();
        while (true) {
            Move move = picker.pickNextMove();
            if (move == null) break;
            moves.add(move);
        }

        Assertions.assertEquals(4, moves.size());
    }

    @Test
    public void testMovePickerPerformance() {

        // 8.7 - 8.8 , 45000

        Instant start = Instant.now();
        int totalSwaps = 0;

        for (int i = 0; i < 3000; i++) {
            for (String fen : Bench.FENS) {
                Board board = FEN.toBoard(fen);
                MovePicker picker = new MovePicker(moveGenerator, moveOrderer, board, 0);
                while (true) {
                    Move move = picker.pickNextMove();
                    totalSwaps += picker.swaps;
                    if (move == null) break;
                }
            }
        }

        Instant end = Instant.now();
        System.out.println("Total time: " + (end.toEpochMilli() - start.toEpochMilli()) + "ms, swaps: " + totalSwaps);

    }

}