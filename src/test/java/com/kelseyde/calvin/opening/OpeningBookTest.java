package com.kelseyde.calvin.opening;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.engine.EngineInitializer;
import com.kelseyde.calvin.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OpeningBookTest {

    @Test
    public void testOpeningBook() {

        OpeningBook book = EngineInitializer.loadDefaultOpeningBook(TestUtils.PRD_CONFIG);
        Assertions.assertNotNull(book);

        Board board = new Board();
        Assertions.assertTrue(book.hasBookMove(board.getGameState().getZobrist()));


    }

}