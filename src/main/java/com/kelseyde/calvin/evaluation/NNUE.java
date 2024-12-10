package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.*;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.evaluation.activation.Activation;
import com.kelseyde.calvin.search.Search;
import com.kelseyde.calvin.uci.UCI;

/**
 * Calvin's evaluation function is an Efficiently Updatable Neural Network (NNUE).
 * <p>
 * The network has an input layer of 768 neurons, each representing the presence of a piece of each colour on a square
 * (64 squares * 6 pieces * 2 colours). Two versions of the hidden layer are accumulated: one from white's perspective
 * and one from black's. It is 'efficiently updatable' due to the fact that, on each move, only the features of the
 * relevant pieces need to be re-calculated, not the features of the entire board; this is a significant speed boost.
 * <p>
 * The network was trained on positions taken from a dataset of Leela Chess Zero, which were then re-scored with
 * Calvin's own search and hand-crafted evaluation.
 *
 * @see <a href="https://www.chessprogramming.org/NNUE">Chess Programming Wiki</a>
 */
public class NNUE {

    public static final Network NETWORK = Network.builder()
            .file("calvin1024.nnue")
            .inputSize(768)
            .hiddenSize(1024)
            .activation(Activation.SCReLU)
            .horizontalMirror(true)
            .quantisations(new int[]{255, 64})
            .scale(400)
            .build();

    private Accumulator[] accumulatorStack;
    private int current;
    private Board board;

    public NNUE() {
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];
        this.accumulatorStack[current] = new Accumulator(NETWORK.hiddenSize());
    }

    public NNUE(Board board) {
        this.board = board;
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];
        this.accumulatorStack[current] = new Accumulator(NETWORK.hiddenSize());
        activateAll(board);
    }

    public int evaluate() {

        final boolean white = board.isWhite();
        final Accumulator acc = accumulatorStack[current];

        // Get the 'us-perspective' and 'them-perspective' feature sets, based on the side to move.
        final short[] us = white ? acc.whiteFeatures : acc.blackFeatures;
        final short[] them = white ? acc.blackFeatures : acc.whiteFeatures;

        // Pass the features through the network to get the evaluation.
        int eval = NETWORK.activation().forward(us, them);

        // Scale the evaluation based on the material and proximity to 50-move rule draw.
        eval = scaleEvaluation(board, eval);

        return eval;

    }

    private void activateAll(Board board) {
        final Accumulator acc = accumulatorStack[current];
        fullRefresh(board, acc, true);
        fullRefresh(board, acc, false);
    }

    private void fullRefresh(Board board, Accumulator acc, boolean whitePerspective) {
        final int kingSquare = board.kingSquare(whitePerspective);
        final boolean mirror = NETWORK.horizontalMirror() && shouldMirror(kingSquare);
        fullRefresh(board, acc, whitePerspective, mirror);
    }

    private void fullRefresh(Board board, Accumulator acc, boolean whitePerspective, boolean mirror) {
        acc.reset(whitePerspective);
        long pieces = board.getOccupied();
        while (pieces != 0) {
            int square = Bits.next(pieces);
            final Piece piece = board.pieceAt(square);
            final boolean whitePiece = Bits.contains(board.getWhitePieces(), square);
            final int index = featureIndex(piece, square, mirror, whitePiece, whitePerspective);
            acc.add(index, whitePerspective);
            pieces = Bits.pop(pieces);
        }
    }

    /**
     * Efficiently update only the relevant features of the network after a move has been made.
     */
    public void makeMove(Board board, Move move) {

        Accumulator acc = accumulatorStack[current];
        final boolean white = board.isWhite();
        final int from = move.from();
        final int to = move.to();
        final Piece piece = board.pieceAt(from);
        if (piece == null) return;

        final int whiteKingSquare = board.kingSquare(true);
        final int blackKingSquare = board.kingSquare(false);
        boolean whiteMirror = NETWORK.horizontalMirror() && shouldMirror(whiteKingSquare);
        boolean blackMirror = NETWORK.horizontalMirror() && shouldMirror(blackKingSquare);

        Accumulator newAcc;
        if (mustRefresh(board, move, piece)) {
            newAcc = acc.copy();
            if (white) whiteMirror = !whiteMirror;
            else blackMirror = !blackMirror;

            boolean mirror = white ? whiteMirror : blackMirror;
            fullRefresh(board, newAcc, white, mirror);
            acc = newAcc;
        }

        final Piece newPiece = move.isPromotion() ? move.promoPiece() : piece;
        final Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(to);

        if (move.isCastling()) {
            newAcc = handleCastleMove(acc, move, whiteMirror, blackMirror, white);
        }
        else if (captured != null) {
            newAcc = handleCapture(acc, move, piece, newPiece, captured, whiteMirror, blackMirror, white);
        }
        else {
            newAcc = handleStandardMove(acc, move, piece, newPiece, whiteMirror, blackMirror, white);
        }
        accumulatorStack[++current] = newAcc;
    }

    private Accumulator handleStandardMove(Accumulator acc, Move move, Piece piece, Piece newPiece, boolean whiteMirror, boolean blackMirror, boolean white) {
        final int wSub = featureIndex(piece, move.from(), whiteMirror, white, true);
        final int bSub = featureIndex(piece, move.from(), blackMirror, white, false);

        final int wAdd = featureIndex(newPiece, move.to(), whiteMirror, white, true);
        final int bAdd = featureIndex(newPiece, move.to(), blackMirror, white, false);

        return acc.addSub(wAdd, bAdd, wSub, bSub);
    }

    private Accumulator handleCastleMove(Accumulator acc, Move move, boolean whiteMirror, boolean blackMirror, boolean white) {
        final boolean kingside = Castling.isKingside(move.from(), move.to());

        // In Chess960, castling is encoded as 'king captures rook'.
        final int kingTo = UCI.Options.chess960 ? Castling.kingTo(kingside, white) : move.to();
        final int rookFrom = UCI.Options.chess960 ? move.to() : Castling.rookFrom(kingside, white);
        final int rookTo = Castling.rookTo(kingside, white);

        final int wSub1 = featureIndex(Piece.KING, move.from(), whiteMirror, white, true);
        final int bSub1 = featureIndex(Piece.KING, move.from(), blackMirror, white, false);
        final int wAdd1 = featureIndex(Piece.KING, kingTo, whiteMirror, white, true);
        final int bAdd1 = featureIndex(Piece.KING, kingTo, blackMirror, white, false);

        final int wSub2 = featureIndex(Piece.ROOK, rookFrom, whiteMirror, white, true);
        final int bSub2 = featureIndex(Piece.ROOK, rookFrom, blackMirror, white, false);
        final int wAdd2 = featureIndex(Piece.ROOK, rookTo, whiteMirror, white, true);
        final int bAdd2 = featureIndex(Piece.ROOK, rookTo, blackMirror, white, false);

        return acc.addAddSubSub(wAdd1, bAdd1, wAdd2, bAdd2, wSub1, bSub1, wSub2, bSub2);
    }

    private Accumulator handleCapture(
            Accumulator acc, Move move, Piece piece, Piece newPiece, Piece captured, boolean whiteMirror, boolean blackMirror, boolean white) {
        final int wSub1 = featureIndex(piece, move.from(), whiteMirror, white, true);
        final int bSub1 = featureIndex(piece, move.from(), blackMirror, white, false);

        final int wAdd1 = featureIndex(newPiece, move.to(), whiteMirror, white, true);
        final int bAdd1 = featureIndex(newPiece, move.to(), blackMirror, white, false);

        int captureSquare = move.to();
        if (move.isEnPassant()) captureSquare = white ? move.to() - 8 : move.to() + 8;

        final int wSub2 = featureIndex(captured, captureSquare, whiteMirror, !white, true);
        final int bSub2 = featureIndex(captured, captureSquare, blackMirror, !white, false);

        return acc.addSubSub(wAdd1, bAdd1, wSub1, bSub1, wSub2, bSub2);
    }

    public void unmakeMove() {
        current--;
    }

    public void setPosition(Board board) {
        clearHistory();
        this.board = board;
        activateAll(board);
    }

    private int scaleEvaluation(Board board, int eval) {

        // Scale down the evaluation when there's not much material left on the board - this creates an incentive
        // to keep pieces on the board when we have winning chances, and trade them off when we're under pressure.
        final int materialPhase = materialPhase(board);
        eval = eval * (22400 + materialPhase) / 32768;

        // Scale down the evaluation as we approach the 50-move rule draw - this gives the engine an understanding
        // of when no progress is being made in the position.
        eval = eval * (200 - board.getState().getHalfMoveClock()) / 200;

        return eval;

    }

    private int materialPhase(Board board) {
        final int knights = Bits.count(board.getKnights());
        final int bishops = Bits.count(board.getBishops());
        final int rooks = Bits.count(board.getRooks());
        final int queens = Bits.count(board.getQueens());
        return 3 * knights + 3 * bishops + 5 * rooks + 10 * queens;
    }

    private boolean mustRefresh(Board board, Move move, Piece piece) {
        if (!NETWORK.horizontalMirror()) return false;
        if (piece != Piece.KING) return false;
        int prevKingSquare = move.from();
        int currKingSquare = move.to();
        if (move.isCastling() && UCI.Options.chess960) {
            final boolean kingside = Castling.isKingside(move.from(), move.to());
            currKingSquare = Castling.kingTo(kingside, board.isWhite());
        }
        return shouldMirror(prevKingSquare) != shouldMirror(currKingSquare);
    }

    private boolean shouldMirror(int kingSquare) {
        return File.of(kingSquare) > 3;
    }

    /**
     * Compute the index of the feature vector for a given piece, colour and square. Features from black's perspective
     * are mirrored (the square index is vertically flipped) in order to preserve symmetry.
     */
    private static int featureIndex(Piece piece, int square, boolean mirror, boolean whitePiece, boolean whitePerspective) {
        int squareIndex = whitePerspective ? square : Square.flipRank(square);
        if (mirror) squareIndex = Square.flipFile(squareIndex);
        final int pieceIndex = piece.index();
        final int pieceOffset = pieceIndex * Square.COUNT;
        final boolean ourPiece = whitePiece == whitePerspective;
        final int colourOffset = ourPiece ? 0 : (Square.COUNT * Piece.COUNT);
        return colourOffset + pieceOffset + squareIndex;
    }

    public void clearHistory() {
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];
        this.accumulatorStack[0] = new Accumulator(NETWORK.hiddenSize());
    }

}
