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
        Assertions.assertEquals(newBoard.pawns(true), fenBoard.pawns(true));
        Assertions.assertEquals(newBoard.knights(true), fenBoard.knights(true));
        Assertions.assertEquals(newBoard.bishops(true), fenBoard.bishops(true));
        Assertions.assertEquals(newBoard.rooks(true), fenBoard.rooks(true));
        Assertions.assertEquals(newBoard.queens(true), fenBoard.queens(true));
        Assertions.assertEquals(newBoard.king(true), fenBoard.king(true));
        Assertions.assertEquals(newBoard.pawns(false), fenBoard.pawns(false));
        Assertions.assertEquals(newBoard.knights(false), fenBoard.knights(false));
        Assertions.assertEquals(newBoard.bishops(false), fenBoard.bishops(false));
        Assertions.assertEquals(newBoard.rooks(false), fenBoard.rooks(false));
        Assertions.assertEquals(newBoard.queens(false), fenBoard.queens(false));
        Assertions.assertEquals(newBoard.king(false), fenBoard.king(false));

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