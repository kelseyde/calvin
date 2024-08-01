package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Bitwise;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.engine.EngineInitializer;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Implementation of {@link Evaluation} using an NNUE (Efficiently Updatable Neural Network) evaluation function.
 * <p>
 * The network has an input layer of 768 neurons, each representing the presence of a piece of each colour on a square
 * (64 squares * 6 pieces * 2 colours). Two versions of the hidden layer are accumulated: one from white's perspective
 * and one from black's. It is 'efficiently updatable' due to the fact that, on each move, only the features of the
 * relevant pieces need to be re-calculated, not the features of the entire board; this is a significant speed boost.
 * <p>
 * The network was trained on positions taken from a dataset of Leela Chess Zero, which were then re-scored with
 * Calvin's own search and hand-crafted evaluation.
 *
 * @see <a href="https://www.chessprogramming.org/UCI">Chess Programming Wiki</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NNUE implements Evaluation {

    public record Network(short[] inputWeights, short[] inputBiases, short[] outputWeights, short outputBias) {

        public static final String FILE = "woodpusher.nnue";
        public static final int INPUT_SIZE = 768;
        public static final int HIDDEN_SIZE = 256;

        public static final Network NETWORK = EngineInitializer.loadNetwork(FILE, INPUT_SIZE, HIDDEN_SIZE);

    }

    static final int COLOUR_OFFSET = 64 * 6;
    static final int PIECE_OFFSET = 64;
    static final int SCALE = 400;

    static final int QA = 255;
    static final int QB = 64;
    static final int QAB = QA * QB;

    final Deque<Accumulator> accumulatorHistory = new ArrayDeque<>();
    final Deque<AccumulatorUpdate> updates = new ArrayDeque<>();
    Accumulator accumulator;
    Board board;

    public NNUE() {
        this.accumulator = new Accumulator(Network.HIDDEN_SIZE);
    }

    public NNUE(Board board) {
        this.accumulator = new Accumulator(Network.HIDDEN_SIZE);
        this.board = board;
        activateAll(board);
    }

    @Override
    public int evaluate() {

        applyLazyUpdates();

        boolean white = board.isWhiteToMove();
        short[] us = white ? accumulator.whiteFeatures : accumulator.blackFeatures;
        short[] them = white ? accumulator.blackFeatures : accumulator.whiteFeatures;
        int eval = Network.NETWORK.outputBias();
        eval += forward(us, 0);
        eval += forward(them, Network.HIDDEN_SIZE);
        eval *= SCALE;
        eval /= QAB;
        return eval;

    }

    /**
     * Forward pass through the network, using the clipped ReLU activation function.
     * Implementation uses the Java Vector API to perform SIMD operations on multiple features at once.
     */
    private int forward(short[] features, int weightOffset) {
        short[] weights = Network.NETWORK.outputWeights;
        short floor = 0;
        short ceil = QA;
        int sum = 0;

        VectorSpecies<Short> species = ShortVector.SPECIES_PREFERRED;
        for (int i = 0; i < species.loopBound(features.length); i += species.length()) {
            var featuresVector = ShortVector.fromArray(species, features, i);
            var weightsVector = ShortVector.fromArray(species, weights, i + weightOffset);

            var clippedVector = featuresVector.min(ceil).max(floor);
            var resultVector = clippedVector.mul(weightsVector);

            sum = Math.addExact(sum,resultVector.reduceLanes(VectorOperators.ADD));
        }

        return sum;
    }

    private void activateAll(Board board) {

        for (int i = 0; i < Network.HIDDEN_SIZE; i++) {
            accumulator.whiteFeatures[i] = Network.NETWORK.inputBiases()[i];
            accumulator.blackFeatures[i] = Network.NETWORK.inputBiases()[i];
        }

        activateSide(board, board.getWhitePieces(), true);
        activateSide(board, board.getBlackPieces(), false);

    }

    private void activateSide(Board board, long pieces, boolean white) {
        while (pieces != 0) {
            int square = Bitwise.getNextBit(pieces);
            Piece piece = board.pieceAt(square);
            int whiteIndex = featureIndex(piece, square, white, true);
            int blackIndex = featureIndex(piece, square, white, false);
            accumulator.add(whiteIndex, blackIndex);
            pieces = Bitwise.popBit(pieces);
        }
    }

    /**
     * Efficiently update only the relevant features of the network after a move has been made.
     */
    @Override
    public void makeMove(Board board, Move move) {
        boolean white = board.isWhiteToMove();
        int startSquare = move.getStartSquare();
        int endSquare = move.getEndSquare();
        Piece piece = board.pieceAt(startSquare);
        if (piece == null) return;
        Piece newPiece = move.isPromotion() ? move.getPromotionPiece() : piece;
        Piece capturedPiece = move.isEnPassant() ? Piece.PAWN : board.pieceAt(endSquare);

        AccumulatorUpdate update;
        if (move.isCastling()) {
            update = handleCastleMove(move, white);
        } else if (capturedPiece != null) {
            update = handleCapture(move, piece, newPiece, capturedPiece, white);
        } else {
            update = handleStandardMove(move, piece, newPiece, white);
        }
        updates.push(update);
    }

    private AccumulatorUpdate handleStandardMove(Move move, Piece piece, Piece newPiece, boolean white) {
        AccumulatorUpdate update = new AccumulatorUpdate();
        FeatureUpdate add = new FeatureUpdate(move.getEndSquare(), newPiece, white);
        FeatureUpdate sub = new FeatureUpdate(move.getStartSquare(), piece, white);
        update.pushAddSub(add, sub);
        return update;
    }

    private AccumulatorUpdate handleCastleMove(Move move, boolean white) {
        boolean kingside = Board.file(move.getEndSquare()) == 6;
        int rookStart = kingside ? white ? 7 : 63 : white ? 0 : 56;
        int rookEnd = kingside ? white ? 5 : 61 : white ? 3 : 59;
        FeatureUpdate kingAdd = new FeatureUpdate(move.getEndSquare(), Piece.KING, white);
        FeatureUpdate kingSub = new FeatureUpdate(move.getStartSquare(), Piece.KING, white);
        FeatureUpdate rookAdd = new FeatureUpdate(rookEnd, Piece.ROOK, white);
        FeatureUpdate rookSub = new FeatureUpdate(rookStart, Piece.ROOK, white);
        AccumulatorUpdate update = new AccumulatorUpdate();
        update.pushAddAddSubSub(kingAdd, rookAdd, kingSub, rookSub);
        return update;
    }

    private AccumulatorUpdate handleCapture(Move move, Piece piece, Piece newPiece, Piece capturedPiece, boolean white) {
        int captureSquare = move.getEndSquare();
        if (move.isEnPassant()) captureSquare = white ? move.getEndSquare() - 8 : move.getEndSquare() + 8;
        AccumulatorUpdate update = new AccumulatorUpdate();
        FeatureUpdate add = new FeatureUpdate(move.getEndSquare(), newPiece, white);
        FeatureUpdate sub1 = new FeatureUpdate(captureSquare, capturedPiece, !white);
        FeatureUpdate sub2 = new FeatureUpdate(move.getStartSquare(), piece, white);
        update.pushAddSubSub(add, sub1, sub2);
        return update;
    }

    private void applyLazyUpdates() {
        while (!updates.isEmpty()) {
            this.accumulatorHistory.push(accumulator.copy());
            AccumulatorUpdate update = updates.pop();
            if (update.addCount == 1 && update.subCount == 1) {
                lazyUpdateAddSub(update);
            }
            else if (update.addCount == 1 && update.subCount == 2) {
                lazyUpdateAddSubSub(update);
            }
            else if (update.addCount == 2 && update.subCount == 2) {
                lazyUpdateAddAddSubSub(update);
            }
            else {
                throw new IllegalStateException("Invalid update");
            }
        }
    }

    private void lazyUpdateAddSub(AccumulatorUpdate update) {
        FeatureUpdate add = update.adds[0];
        FeatureUpdate sub = update.subs[0];
        int whiteAddIdx= featureIndex(add.piece, add.square, add.white, true);
        int blackAddIdx = featureIndex(add.piece, add.square, add.white, false);
        int whiteSubIdx = featureIndex(sub.piece, sub.square, sub.white, true);
        int blackSubIdx = featureIndex(sub.piece, sub.square, sub.white, false);
        accumulator.addSub(whiteAddIdx, blackAddIdx, whiteSubIdx, blackSubIdx);
    }

    private void lazyUpdateAddSubSub(AccumulatorUpdate update) {
        FeatureUpdate add1 = update.adds[0];
        FeatureUpdate sub1 = update.subs[0];
        FeatureUpdate sub2 = update.subs[1];
        int whiteAdd1Idx = featureIndex(add1.piece, add1.square, add1.white, true);
        int blackAdd1Idx = featureIndex(add1.piece, add1.square, add1.white, false);
        int whiteSub1Idx = featureIndex(sub1.piece, sub1.square, sub1.white, true);
        int blackSub1Idx = featureIndex(sub1.piece, sub1.square, sub1.white, false);
        int whiteSub2Idx = featureIndex(sub2.piece, sub2.square, sub2.white, true);
        int blackSub2Idx = featureIndex(sub2.piece, sub2.square, sub2.white, false);
        accumulator.addSubSub(whiteAdd1Idx, blackAdd1Idx, whiteSub1Idx, blackSub1Idx, whiteSub2Idx, blackSub2Idx);
    }

    private void lazyUpdateAddAddSubSub(AccumulatorUpdate update) {
        FeatureUpdate add1 = update.adds[0];
        FeatureUpdate add2 = update.adds[1];
        FeatureUpdate sub1 = update.subs[0];
        FeatureUpdate sub2 = update.subs[1];
        int whiteAdd1Idx = featureIndex(add1.piece, add1.square, add1.white, true);
        int blackAdd1Idx = featureIndex(add1.piece, add1.square, add1.white, false);
        int whiteAdd2Idx = featureIndex(add2.piece, add2.square, add2.white, true);
        int blackAdd2Idx = featureIndex(add2.piece, add2.square, add2.white, false);
        int whiteSub1Idx = featureIndex(sub1.piece, sub1.square, sub1.white, true);
        int blackSub1Idx = featureIndex(sub1.piece, sub1.square, sub1.white, false);
        int whiteSub2Idx = featureIndex(sub2.piece, sub2.square, sub2.white, true);
        int blackSub2Idx = featureIndex(sub2.piece, sub2.square, sub2.white, false);
        accumulator.addAddSubSub(whiteAdd1Idx, blackAdd1Idx, whiteAdd2Idx, blackAdd2Idx, whiteSub1Idx, blackSub1Idx, whiteSub2Idx, blackSub2Idx);
    }

    @Override
    public void unmakeMove() {
        if (!this.updates.isEmpty()) {
            this.updates.pop();
        } else {
            this.accumulator = accumulatorHistory.pop();
        }
    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        activateAll(board);
    }

    /**
     * Compute the index of the feature vector for a given piece, colour and square. Features from black's perspective
     * are mirrored (the square index is vertically flipped) in order to preserve symmetry.
     */
    private static int featureIndex(Piece piece, int square, boolean whitePiece, boolean whitePerspective) {
        int squareIndex = whitePerspective ? square : square ^ 56;
        int pieceIndex = piece.getIndex();
        int pieceOffset = pieceIndex * PIECE_OFFSET;
        boolean ourPiece = whitePiece == whitePerspective;
        int colourOffset = ourPiece ? 0 : COLOUR_OFFSET;
        return colourOffset + pieceOffset + squareIndex;
    }

    @Override
    public void clearHistory() {
        this.accumulator = new Accumulator(Network.HIDDEN_SIZE);
        this.accumulatorHistory.clear();
        this.updates.clear();
    }

    private record FeatureUpdate(int square, Piece piece, boolean white) {}

    private static class AccumulatorUpdate {

        private FeatureUpdate[] adds = new FeatureUpdate[2];
        private int addCount = 0;

        private FeatureUpdate[] subs = new FeatureUpdate[2];
        private int subCount = 0;

        public void pushAdd(FeatureUpdate update) {
            adds[addCount++] = update;
        }

        public void pushSub(FeatureUpdate update) {
            subs[subCount++] = update;
        }

        public void pushAddSub(FeatureUpdate add, FeatureUpdate sub) {
            pushAdd(add);
            pushSub(sub);
        }

        public void pushAddSubSub(FeatureUpdate add, FeatureUpdate sub1, FeatureUpdate sub2) {
            pushAdd(add);
            pushSub(sub1);
            pushSub(sub2);
        }

        public void pushAddAddSubSub(FeatureUpdate add1, FeatureUpdate add2, FeatureUpdate sub1, FeatureUpdate sub2) {
            pushAdd(add1);
            pushAdd(add2);
            pushSub(sub1);
            pushSub(sub2);
        }

    }

}
