package com.kelseyde.calvin.evaluation;

import com.kelseyde.calvin.board.*;
import com.kelseyde.calvin.board.Bits.File;
import com.kelseyde.calvin.evaluation.Accumulator.AccumulatorUpdate;
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

    public Accumulator[] accumulatorStack;
    private int current;
    private Board board;

    public NNUE() {
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];
        this.accumulatorStack[current] = new Accumulator(false, false);
    }

    public NNUE(Board board) {
        this.board = board;
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];

        boolean whiteMirror = shouldMirror(board.kingSquare(true));
        boolean blackMirror = shouldMirror(board.kingSquare(false));
        this.accumulatorStack[current] = new Accumulator(whiteMirror, blackMirror);
        activateAll(board);
    }

    public int evaluate() {

        applyLazyUpdates();

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
            final Feature feature = new Feature(piece, square, whitePiece);
            acc.add(feature, whitePerspective, mirror);
            pieces = Bits.pop(pieces);
        }
    }

    public void makeMove(Board board, Move move) {

        final Accumulator acc = accumulatorStack[++current] = new Accumulator();
        final boolean white = board.isWhite();

        acc.computed[Colour.WHITE] = false;
        acc.computed[Colour.BLACK] = false;

        acc.mirror[Colour.WHITE] = shouldMirror(board.kingSquare(true));
        acc.mirror[Colour.BLACK] = shouldMirror(board.kingSquare(false));

        boolean needsRefresh = needsRefresh(board, move);
        acc.needsRefresh[Colour.index(white)] = needsRefresh;

        acc.update = switch (moveType(board, move)) {
            case STANDARD -> handleStandardMove(board, move, white);
            case CAPTURE -> handleCapture(board, move, white);
            case CASTLE -> handleCastleMove(move, white);
        };

    }

    private AccumulatorUpdate handleStandardMove(Board board, Move move, boolean white) {

        final Piece piece = board.pieceAt(move.from());
        final Piece newPiece = move.isPromotion() ? move.promoPiece() : piece;

        AccumulatorUpdate update = new AccumulatorUpdate();
        update.pushAdd(new Feature(newPiece, move.to(), white));
        update.pushSub(new Feature(piece, move.from(), white));
        return update;

    }

    private AccumulatorUpdate handleCastleMove(Move move, boolean white) {

        AccumulatorUpdate update = new AccumulatorUpdate();
        final boolean kingside = Castling.isKingside(move.from(), move.to());

        // In Chess960, castling is encoded as 'king captures rook'.
        final int kingTo = UCI.Options.chess960 ? Castling.kingTo(kingside, white) : move.to();
        final int rookFrom = UCI.Options.chess960 ? move.to() : Castling.rookFrom(kingside, white);
        final int rookTo = Castling.rookTo(kingside, white);

        update.pushSub(new Feature(Piece.KING, move.from(), white));
        update.pushSub(new Feature(Piece.ROOK, rookFrom, white));
        update.pushAdd(new Feature(Piece.KING, kingTo, white));
        update.pushAdd(new Feature(Piece.ROOK, rookTo, white));

        return update;

    }

    private AccumulatorUpdate handleCapture(Board board, Move move, boolean white) {

        final Piece piece = board.pieceAt(move.from());
        final Piece newPiece = move.isPromotion() ? move.promoPiece() : piece;
        final Piece captured = move.isEnPassant() ? Piece.PAWN : board.pieceAt(move.to());

        AccumulatorUpdate update = new AccumulatorUpdate();
        int captureSquare = move.to();
        if (move.isEnPassant()) {
            captureSquare = white ? move.to() - 8 : move.to() + 8;
        }
        update.pushSub(new Feature(piece, move.from(), white));
        update.pushAdd(new Feature(newPiece, move.to(), white));
        update.pushSub(new Feature(captured, captureSquare, !white));
        return update;

    }

    public void unmakeMove() {
        current--;
    }

    private void applyLazyUpdates() {
        applyLazyUpdates(true);
        applyLazyUpdates(false);
    }

    // Implementation based on Stormphrax:
    // https://github.com/Ciekce/Stormphrax/commit/9b76f2a35531513239ed7078acc21294a11e75c6
    private void applyLazyUpdates(boolean whitePerspective) {

        Accumulator acc = accumulatorStack[current];

        int colourIndex = Colour.index(whitePerspective);

        if (current == 0) {
            fullRefresh(board, acc, whitePerspective);
            return;
        }

        // If the current state is correct for our perspective, no work is required.
        if (acc.computed[colourIndex]) {
            return;
        }

        // If the current state requires a full refresh, don't bother with previous states
        if (acc.needsRefresh[Colour.index(whitePerspective)]) {
            fullRefresh(board, acc, whitePerspective);
            return;
        }

        // Find the most recent accumulator that is computed or requires a refresh.
        int index = current - 1;
        while (index > 0
                && !accumulatorStack[index].computed[colourIndex]
                && !accumulatorStack[index].needsRefresh[colourIndex]) {
            index--;
        }

        Accumulator prev = accumulatorStack[index];
        if (prev.needsRefresh[colourIndex]) {
            // The previous accumulator would require a full refresh; therefore just refresh the current one instead
            fullRefresh(board, acc, whitePerspective);
        }
        else {
            // Apply the updates from the previous state to the current state
            while (index < current) {
                Accumulator curr = accumulatorStack[++index];
                curr.copyFrom(prev, whitePerspective);
                curr.apply(curr.update, whitePerspective);
                curr.computed[colourIndex] = true;
            }

        }

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

    private boolean needsRefresh(Board board, Move move) {
        if (!NETWORK.horizontalMirror()) return false;
        final Piece piece = board.pieceAt(move.from());
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
        return NETWORK.horizontalMirror() && File.of(kingSquare) > 3;
    }

    private MoveType moveType(Board board, Move move) {
        if (move.isCastling()) return MoveType.CASTLE;
        if (board.isCapture(move)) return MoveType.CAPTURE;
        return MoveType.STANDARD;
    }

    public void clearHistory() {
        this.current = 0;
        this.accumulatorStack = new Accumulator[Search.MAX_DEPTH];

        boolean whiteMirror = board != null && shouldMirror(board.kingSquare(true));
        boolean blackMirror = board != null && shouldMirror(board.kingSquare(false));
        this.accumulatorStack[0] = new Accumulator(whiteMirror, blackMirror);
    }

    public enum MoveType {
        STANDARD,
        CAPTURE,
        CASTLE
    }

}
