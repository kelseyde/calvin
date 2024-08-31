package com.kelseyde.calvin.opening;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simple implementation of an opening book that retrieves moves from a .txt file of FEN positions from grandmaster games.
 * Each position has >= 1 moves, weighted by the number of times that move has been played. The move is selected semi-randomly,
 * with a weighted bias towards the moves that have been played the most frequently.
 * @see <a href="https://www.chessprogramming.org/Opening_Book">Chess Programming Wiki</a>
 * </p>
 * The opening book is enabled by default. It can be disabled via the 'OwnBook' UCI command.
 * </p>
 * The default opening book is borrowed from Sebastian Lague's Chess Coding Adventure.
 * @see <a href="https://github.com/SebLague/Chess-Coding-Adventure">Chess Coding Adventure</a>
 */
public class OpeningBook {

    public record BookMove(Move move, int frequency) {}

    private final Map<Long, BookMove[]> movesByPosition;
    private final Random random = new Random();

    public OpeningBook(String file) {
        String[] entries = file.trim().split("pos");
        movesByPosition = new HashMap<>(entries.length);
        for (String entry : entries) {
            if (entry.isBlank()) continue;
            String[] entryData = entry.trim().split("\n");
            String fen = entryData[0];
            long key = FEN.toBoard(fen).getGameState().getZobrist();
            BookMove[] bookMoves = Arrays.stream(entryData, 1, entryData.length)
                    .map(this::parseBookMove)
                    .toArray(BookMove[]::new);
            movesByPosition.put(key, bookMoves);
        }
    }

    public boolean hasBookMove(long key) {
        return movesByPosition.containsKey(key);
    }

    public Move getBookMove(Board board) {
        long key = board.getGameState().getZobrist();
        BookMove[] bookMoves = movesByPosition.get(key);
        if (bookMoves == null) {
            return null;
        }
        if (bookMoves.length == 1) {
            return bookMoves[0].move();
        }
        int totalWeight = Arrays.stream(bookMoves).map(BookMove::frequency).reduce(0, Integer::sum);
        int randomInt = random.nextInt(totalWeight);
        int cumulativeWeight = 0;
        for (BookMove bookMove : bookMoves) {
            cumulativeWeight += bookMove.frequency();
            if (randomInt < cumulativeWeight) {
                return bookMove.move();
            }
        }
        return null;
    }

    private BookMove parseBookMove(String moveEntry) {
        String[] moveData = moveEntry.split(" ");
        Move move = Notation.fromUCI(moveData[0]);
        int frequency = Integer.parseInt(moveData[1]);
        return new BookMove(move, frequency);
    }

}
