package com.kelseyde.calvin.puzzles;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.bot.Bot;
import com.kelseyde.calvin.bot.CalvinBot;
import com.kelseyde.calvin.evaluation.Evaluator;
import com.kelseyde.calvin.movegeneration.result.ResultCalculator;
import com.kelseyde.calvin.utils.NotationUtils;
import com.kelseyde.calvin.utils.fen.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Disabled
public class MiddlegameTest {

    @Test
    public void testEvaluationOfWinningEqualMaterialPosition() {

        String fen = "k6K/1pp2P1P/p1p5/P7/8/8/5r2/2R5 w - - 1 51";
        Board board = FEN.fromFEN(fen);

        Evaluator evaluator = new Evaluator(board);
        System.out.println(evaluator.get());

    }

    @Test
    public void testKingSafetyDuringPawnStorm() {

        String fen = "3r1r1k/pQ1b2pp/4p1q1/2p1b3/2B2p2/2N1B2P/PPP2PP1/3R1RK1 w - - 0 23";
        Bot bot = new CalvinBot();
        bot.setPosition(fen, Collections.emptyList());
        Move move = bot.think(5000);
        System.out.println(NotationUtils.toNotation(move));

    }

}
