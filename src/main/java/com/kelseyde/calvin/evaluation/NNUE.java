package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.Bits;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.board.Bits.Square;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;

import static jdk.incubator.vector.VectorOperators.S2I;

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
public class NNUE implements Evaluation {

    public record Network(short[] inputWeights, short[] inputBiases, short[][] outputWeights, short[] outputBias) {

        public static final String FILE = "sunbeam.nnue";
        public static final int INPUT_SIZE = 768;
        public static final int HIDDEN_SIZE = 384;

        public static final Network NETWORK = loadNetwork(FILE, INPUT_SIZE, HIDDEN_SIZE);

    }

    static final int COLOUR_OFFSET = Square.COUNT * Piece.COUNT;
    static final int PIECE_OFFSET = Square.COUNT;
    static final int SCALE = 400;

    static final int QA = 255;
    static final int QB = 64;
    static final int QAB = QA * QB;

    static final int OUTPUT_BUCKETS = 8;

    static final int MATERIAL_BASE = 22400;
    static final int MATERIAL_FACTOR = 32768;

    static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;
    static final int UPPER_BOUND = SPECIES.loopBound(Network.HIDDEN_SIZE);
    static final int LOOP_LENGTH = SPECIES.length();

    static final ShortVector FLOOR = ShortVector.broadcast(SPECIES, 0);
    static final ShortVector CEIL = ShortVector.broadcast(SPECIES, QA);

    // TODO test using array with single allocation at startup
    final Deque<Accumulator> accumulatorHistory = new ArrayDeque<>();
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

        boolean white = board.isWhite();
        int outputBucket = bucketIndex(board);
        short[] outputWeights = Network.NETWORK.outputWeights[outputBucket];
        short outputBias = Network.NETWORK.outputBias[outputBucket];

        // Get the 'us-perspective' and 'them-perspective' feature sets, based on the side to move.
        short[] us = white ? accumulator.whiteFeatures : accumulator.blackFeatures;
        short[] them = white ? accumulator.blackFeatures : accumulator.whiteFeatures;

        int eval = 0;

        // Forward-pass through the network, using the squared clipped ReLU activation function.
        // Implementation uses the Java Vector API to perform SIMD operations on multiple features at once.
        for (int i = 0; i < UPPER_BOUND; i += LOOP_LENGTH) {

            ShortVector usInputs = ShortVector.fromArray(SPECIES, us, i);
            ShortVector themInputs = ShortVector.fromArray(SPECIES, them, i);
            ShortVector usWeights = ShortVector.fromArray(SPECIES, outputWeights, i);
            ShortVector themWeights = ShortVector.fromArray(SPECIES, outputWeights, i + Network.HIDDEN_SIZE);

            // Clip the inputs to the range [0, 255].
            usInputs = usInputs.max(FLOOR).min(CEIL);
            themInputs = themInputs.max(FLOOR).min(CEIL);

            // Multiply the inputs by the weights.
            ShortVector usTerms = usInputs.mul(usWeights);
            ShortVector themTerms = themInputs.mul(themWeights);

            // Split the inputs and weighted terms into low and high parts, to enable 32-bit multiplication.
            Vector<Integer> usInputsLo = usInputs.convert(S2I, 0);
            Vector<Integer> usInputsHi = usInputs.convert(S2I, 1);
            Vector<Integer> themInputsLo = themInputs.convert(S2I, 0);
            Vector<Integer> themInputsHi = themInputs.convert(S2I, 1);

            Vector<Integer> usTermsLo = usTerms.convert(S2I, 0);
            Vector<Integer> usTermsHi = usTerms.convert(S2I, 1);
            Vector<Integer> themTermsLo = themTerms.convert(S2I, 0);
            Vector<Integer> themTermsHi = themTerms.convert(S2I, 1);

            // Multiply the inputs by the weighted terms, and add the results to the running sum.
            eval += (int) usInputsLo.mul(usTermsLo)
                    .add(usInputsHi.mul(usTermsHi))
                    .add(themInputsLo.mul(themTermsLo))
                    .add(themInputsHi.mul(themTermsHi))
                    .reduceLanesToLong(VectorOperators.ADD);

        }

        // Since squaring the inputs also squares quantisation, we need to divide that out.
        eval /= QA;

        // Add the output bias, scale the result, and divide by the quantisation factor.
        eval += outputBias;
        eval *= SCALE;
        eval /= QAB;

        // Scale the evaluation based on the material and proximity to 50-move rule draw.
        eval = scaleEval(board, eval);

        return eval;

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
            int square = Bits.next(pieces);
            Piece piece = board.pieceAt(square);
            int whiteIndex = featureIndex(piece, square, white, true);
            int blackIndex = featureIndex(piece, square, white, false);
            accumulator.add(whiteIndex, blackIndex);
            pieces = Bits.pop(pieces);
        }
    }

    /**
     * Efficiently update only the relevant features of the network after a move has been made.
     */
    @Override
    public void makeMove(Board board, Move move) {
        accumulatorHistory.push(accumulator.copy());
        boolean white = board.isWhite();
        int from = move.from();
        int to = move.to();
        Piece piece = board.pieceAt(from);
        if (piece == null) return;
        Piece newPiece = move.isPromotion() ? move.promoPiece() : piece;
        Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(to);

        int oldWhiteIdx = featureIndex(piece, from, white, true);
        int oldBlackIdx = featureIndex(piece, from, white, false);

        int newWhiteIdx = featureIndex(newPiece, to, white, true);
        int newBlackIdx = featureIndex(newPiece, to, white, false);

        if (move.isCastling()) {
            handleCastleMove(white, to, oldWhiteIdx, oldBlackIdx, newWhiteIdx, newBlackIdx);
        } else if (captured != null) {
            handleCapture(move, captured, white, newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx);
        } else {
            accumulator.addSub(newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx);
        }
    }

    private void handleCastleMove(boolean white, int to, int oldWhiteIdx, int oldBlackIdx, int newWhiteIdx, int newBlackIdx) {
        boolean isKingside = File.of(to) == 6;
        int rookStart = isKingside ? white ? 7 : 63 : white ? 0 : 56;
        int rookEnd = isKingside ? white ? 5 : 61 : white ? 3 : 59;
        int rookStartWhiteIdx = featureIndex(Piece.ROOK, rookStart, white, true);
        int rookStartBlackIdx = featureIndex(Piece.ROOK, rookStart, white, false);
        int rookEndWhiteIdx = featureIndex(Piece.ROOK, rookEnd, white, true);
        int rookEndBlackIdx = featureIndex(Piece.ROOK, rookEnd, white, false);
        accumulator.addAddSubSub(newWhiteIdx, newBlackIdx, rookEndWhiteIdx, rookEndBlackIdx, oldWhiteIdx, oldBlackIdx, rookStartWhiteIdx, rookStartBlackIdx);
    }

    private void handleCapture(Move move, Piece captured, boolean white, int newWhiteIdx, int newBlackIdx, int oldWhiteIdx, int oldBlackIdx) {
        int captureSquare = move.to();
        if (move.isEnPassant()) captureSquare = white ? move.to() - 8 : move.to() + 8;
        int capturedWhiteIdx = featureIndex(captured, captureSquare, !white, true);
        int capturedBlackIdx = featureIndex(captured, captureSquare, !white, false);
        accumulator.addSubSub(newWhiteIdx, newBlackIdx, oldWhiteIdx, oldBlackIdx, capturedWhiteIdx, capturedBlackIdx);
    }

    @Override
    public void unmakeMove() {
        this.accumulator = accumulatorHistory.pop();
    }

    @Override
    public void setPosition(Board board) {
        this.board = board;
        activateAll(board);
    }

    private int scaleEval(Board board, int eval) {
        //int materialPhase = materialPhase(board);
        //eval = eval * materialPhase / MATERIAL_FACTOR;
        eval = eval * (200 - board.getState().getHalfMoveClock()) / 200;
        return eval;
    }

    private int materialPhase(Board board) {
        long knights = Bits.count(board.getKnights());
        long bishops = Bits.count(board.getBishops());
        long rooks = Bits.count(board.getRooks());
        long queens = Bits.count(board.getQueens());
        return (int) (MATERIAL_BASE + 3 * knights + 3 * bishops + 5 * rooks + 10 * queens);
    }

    /**
     * Compute the index of the feature vector for a given piece, colour and square. Features from black's perspective
     * are mirrored (the square index is vertically flipped) in order to preserve symmetry.
     */
    private static int featureIndex(Piece piece, int square, boolean whitePiece, boolean whitePerspective) {
        int squareIndex = whitePerspective ? square : square ^ 56;
        int pieceIndex = piece.index();
        int pieceOffset = pieceIndex * PIECE_OFFSET;
        boolean ourPiece = whitePiece == whitePerspective;
        int colourOffset = ourPiece ? 0 : COLOUR_OFFSET;
        return colourOffset + pieceOffset + squareIndex;
    }

    private static int bucketIndex(Board board) {
        int material = Bits.count(board.getOccupied());
        return (material - 2) / 4;
    }


    @Override
    public void clearHistory() {
        this.accumulator = new Accumulator(Network.HIDDEN_SIZE);
        this.accumulatorHistory.clear();
    }

    public static NNUE.Network loadNetwork(String file, int inputSize, int hiddenSize) {
        try {
            InputStream inputStream = NNUE.Network.class.getClassLoader().getResourceAsStream(file);
            if (inputStream == null) {
                throw new FileNotFoundException("NNUE file not found in resources");
            }

            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();
            ByteBuffer buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN);

            int inputWeightsOffset = inputSize * hiddenSize;
            int inputBiasesOffset = hiddenSize;
            int outputWeightsOffset = hiddenSize * 2;

            short[] inputWeights = new short[inputWeightsOffset];
            short[] inputBiases = new short[inputBiasesOffset];
            short[][] outputWeights = new short[OUTPUT_BUCKETS][outputWeightsOffset];
            short[] outputBiases = new short[OUTPUT_BUCKETS];

            for (int i = 0; i < inputWeightsOffset; i++) {
                inputWeights[i] = buffer.getShort();
            }

            for (int i = 0; i < inputBiasesOffset; i++) {
                inputBiases[i] = buffer.getShort();
            }

            for (int bucket = 0; bucket < OUTPUT_BUCKETS; bucket++) {
                for (int i = 0; i < hiddenSize * 2; i++) {
                    outputWeights[bucket][i] = buffer.getShort();
                }
            }

            for (int bucket = 0; bucket < OUTPUT_BUCKETS; bucket++) {
                outputBiases[bucket] = buffer.getShort();
            }

            while (buffer.hasRemaining()) {
                if (buffer.getShort() != 0) {
                    throw new RuntimeException("Failed to load NNUE network: invalid file format");
                }
            }

            return new NNUE.Network(inputWeights, inputBiases, outputWeights, outputBiases);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load NNUE network", e);
        }
    }

}
