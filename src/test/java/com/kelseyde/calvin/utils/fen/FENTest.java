package com.kelseyde.calvin.utils.fen;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.utils.FEN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Disabled
public class FENTest {

    @Test
    public void testStartingPosition() {

        Board fenBoard = FEN.toBoard(FEN.STARTPOS);
        Board newBoard = new Board();
        Assertions.assertEquals(newBoard.getPawns(true), fenBoard.getPawns(true));
        Assertions.assertEquals(newBoard.getKnights(true), fenBoard.getKnights(true));
        Assertions.assertEquals(newBoard.getBishops(true), fenBoard.getBishops(true));
        Assertions.assertEquals(newBoard.getRooks(true), fenBoard.getRooks(true));
        Assertions.assertEquals(newBoard.getQueens(true), fenBoard.getQueens(true));
        Assertions.assertEquals(newBoard.getKing(true), fenBoard.getKing(true));
        Assertions.assertEquals(newBoard.getPawns(false), fenBoard.getPawns(false));
        Assertions.assertEquals(newBoard.getKnights(false), fenBoard.getKnights(false));
        Assertions.assertEquals(newBoard.getBishops(false), fenBoard.getBishops(false));
        Assertions.assertEquals(newBoard.getRooks(false), fenBoard.getRooks(false));
        Assertions.assertEquals(newBoard.getQueens(false), fenBoard.getQueens(false));
        Assertions.assertEquals(newBoard.getKing(false), fenBoard.getKing(false));

        Assertions.assertEquals(newBoard.getWhitePieces(), fenBoard.getWhitePieces());
        Assertions.assertEquals(newBoard.getBlackPieces(), fenBoard.getBlackPieces());
        Assertions.assertEquals(newBoard.getOccupied(), fenBoard.getOccupied());

        Assertions.assertEquals(newBoard.isWhiteToMove(), fenBoard.isWhiteToMove());
        Assertions.assertEquals(newBoard.getGameState(), fenBoard.getGameState());
        Assertions.assertEquals(new ArrayList<>(newBoard.getGameStateHistory()), new ArrayList<>(fenBoard.getGameStateHistory()));
        Assertions.assertEquals(new ArrayList<>(newBoard.getMoveHistory()), new ArrayList<>(fenBoard.getMoveHistory()));

    }

    @Test
    public void testAllFens() throws IOException {

        List<String> fens = loadFens();
        for (String originalFen : fens) {
            String actualFen = originalFen.split("\"")[0];
            Board board = FEN.toBoard(actualFen);
            String newFen = FEN.toFEN(board);
            Assertions.assertEquals(actualFen, newFen);
        }

    }

    private List<String> loadFens() throws IOException {
        String fileName = "src/test/resources/texel/quiet_positions.epd";
        Path path = Paths.get(fileName);
        return Files.readAllLines(path);
    }

}