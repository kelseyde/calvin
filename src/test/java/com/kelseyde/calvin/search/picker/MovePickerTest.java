package com.kelseyde.calvin.search.picker;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.movegen.MoveGenerator;
import com.kelseyde.calvin.search.SearchHistory;
import com.kelseyde.calvin.search.SearchStack;
import com.kelseyde.calvin.utils.notation.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class MovePickerTest {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    @Test
    public void testInCheckDoesNotGenerateMovesTwice() {

        String fen = "rnbqkbnr/1p2pppp/p2p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 1";
        Board board = FEN.toBoard(fen);

        MovePicker picker = new MovePicker(moveGenerator, new SearchStack(), new SearchHistory(new EngineConfig()), board, 0, null, true);

        List<ScoredMove> moves = new ArrayList<>();
        while (true) {
            ScoredMove move = picker.pickNextMove();
            if (move == null) break;
            moves.add(move);
        }

        Assertions.assertEquals(5, moves.size());
    }

    @Test
    public void testKillersTriedBeforeBadNoisies() {

        Board board = Board.from("rnb1k1nr/pppp1pp1/4p3/4b1qp/4P3/P1N3P1/1PPP1PBP/R1BQK1NR w KQkq - 0 6");

        SearchHistory history = new SearchHistory(new EngineConfig());
        history.getKillerTable().add(0, Move.fromUCI("d2d4", Move.PAWN_DOUBLE_MOVE_FLAG));
        Move ttMove = Move.fromUCI("d1g4");

        List<Move> triedMoves = new ArrayList<>();

        MovePicker picker = new MovePicker(moveGenerator, new SearchStack(), history, board, 0, ttMove, false);
        while (true) {
            ScoredMove move = picker.pickNextMove();
            if (move == null) break;
            triedMoves.add(move.move());
        }
        Assertions.assertEquals(30, triedMoves.size());
        Assertions.assertEquals(Move.fromUCI("d1g4"), triedMoves.get(0));
        Assertions.assertEquals(Move.fromUCI("d2d4", Move.PAWN_DOUBLE_MOVE_FLAG), triedMoves.get(1));
        Assertions.assertEquals(Move.fromUCI("d1h5"), triedMoves.get(2));

    }

}