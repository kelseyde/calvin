package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bits.Castling;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.evaluation.activation.Activation;
import com.kelseyde.calvin.search.Search;

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

    public static final Network NETWORK = Network.Builder.builder()
            .file("sunbeam.nnue")
            .inputSize(768)
            .hiddenSize(512)
            .activation(Activation.SCReLU)
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
        for (int i = 0; i < NETWORK.hiddenSize(); i++) {
            acc.whiteFeatures[i] = NETWORK.inputBiases()[i];
            acc.blackFeatures[i] = NETWORK.inputBiases()[i];
        }

        activateSide(acc, board, board.getWhitePieces(), true);
        activateSide(acc, board, board.getBlackPieces(), false);

    }

    private void activateSide(Accumulator acc, Board board, long pieces, boolean white) {
        while (pieces != 0) {
            final int square = Bits.next(pieces);
            final Piece piece = board.pieceAt(square);
            final int whiteIndex = featureIndex(piece, square, white, true);
            final int blackIndex = featureIndex(piece, square, white, false);
            acc.add(whiteIndex, blackIndex);
            pieces = Bits.pop(pieces);
        }
    }

    /**
     * Efficiently update only the relevant features of the network after a move has been made.
     */
    public void makeMove(Board board, Move move) {

        final Accumulator acc = accumulatorStack[++current] = accumulatorStack[current - 1].copy();
        final boolean white = board.isWhite();
        final int from = move.from();
        final int to = move.to();
        final Piece piece = board.pieceAt(from);
        if (piece == null) return;
        final Piece newPiece = move.isPromotion() ? move.promoPiece() : piece;
        final Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(to);

        final int oldWhiteIdx = featureIndex(piece, from, white, true);
        final int oldBlackIdx = featureIndex(piece, from, white, false);

        final int newWhiteIdx = featureIndex(newPiece, to, white, true);
        final int newBlackIdx = featureIndex(newPiece, to, white, false);

        if (move.isCastling()) {
            handleCastleMove(acc, white, to, oldWhiteIdx, oldBlackIdx, newWhiteIdx, newBlackIdx);
        } else if (captured != null) {
            handleCapture(acc, move, captured, white, newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx);
        } else {
            acc.addSub(newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx);
        }

    }

    private void handleCastleMove(Accumulator acc, boolean white, int to, int oldWhiteIdx, int oldBlackIdx, int newWhiteIdx, int newBlackIdx) {
        final boolean kingside = File.of(to) == 6;
        final int rookFrom = Castling.rookFrom(kingside, white);
        final int rookTo = Castling.rookTo(kingside, white);
        final int rookStartWhiteIdx = featureIndex(Piece.ROOK, rookFrom, white, true);
        final int rookStartBlackIdx = featureIndex(Piece.ROOK, rookFrom, white, false);
        final int rookEndWhiteIdx = featureIndex(Piece.ROOK, rookTo, white, true);
        final int rookEndBlackIdx = featureIndex(Piece.ROOK, rookTo, white, false);
        acc.addAddSubSub(newWhiteIdx, newBlackIdx, rookEndWhiteIdx, rookEndBlackIdx, oldWhiteIdx, oldBlackIdx, rookStartWhiteIdx, rookStartBlackIdx);
    }

    private void handleCapture(Accumulator acc, Move move, Piece captured, boolean white, int newWhiteIdx, int newBlackIdx, int oldWhiteIdx, int oldBlackIdx) {
        int captureSquare = move.to();
        if (move.isEnPassant()) captureSquare = white ? move.to() - 8 : move.to() + 8;
        final int capturedWhiteIdx = featureIndex(captured, captureSquare, !white, true);
        final int capturedBlackIdx = featureIndex(captured, captureSquare, !white, false);
        acc.addSubSub(newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx, capturedWhiteIdx, capturedBlackIdx);
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
        final long knights = Bits.count(board.getKnights());
        final long bishops = Bits.count(board.getBishops());
        final long rooks = Bits.count(board.getRooks());
        final long queens = Bits.count(board.getQueens());
        return (int) (3 * knights + 3 * bishops + 5 * rooks + 10 * queens);
    }

    /**
     * Compute the index of the feature vector for a given piece, colour and square. Features from black's perspective
     * are mirrored (the square index is vertically flipped) in order to preserve symmetry.
     */
    private static int featureIndex(Piece piece, int square, boolean whitePiece, boolean whitePerspective) {
        final int squareIndex = whitePerspective ? square : square ^ 56;
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
