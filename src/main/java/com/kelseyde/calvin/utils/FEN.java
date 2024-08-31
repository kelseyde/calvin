package com.kelseyde.calvin.utils;

import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.board.Piece;
import com.kelseyde.calvin.board.Zobrist;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FEN {

    public static final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static Board toBoard(String fen) {

            String[] parts = fen.split(" ");
            String[] files = parts[0].split("/");

            long whitePawns = 0L;
            long whiteKnights = 0L;
            long whiteBishops = 0L;
            long whiteRooks = 0L;
            long whiteQueens = 0L;
            long whiteKing = 0L;
            long blackPawns = 0L;
            long blackKnights = 0L;
            long blackBishops = 0L;
            long blackRooks = 0L;
            long blackQueens = 0L;
            long blackKing = 0L;

            List<List<String>> rankFileHash = Arrays.stream(files)
                    .map(file -> Arrays.stream(file.split(""))
                            .flatMap(FEN::parseSquare)
                            .toList())
                    .collect(Collectors.toList());
            Collections.reverse(rankFileHash);

            for (int rankIndex = 0; rankIndex < rankFileHash.size(); rankIndex++) {
                List<String> rank = rankFileHash.get(rankIndex);
                for (int fileIndex = 0; fileIndex < rank.size(); fileIndex++) {
                    int square = Board.squareIndex(rankIndex, fileIndex);
                    String squareValue = rank.get(fileIndex);
                    long squareBB = 1L << square;
                    switch (squareValue) {
                        case "P" -> whitePawns |= squareBB;
                        case "N" -> whiteKnights |= squareBB;
                        case "B" -> whiteBishops |= squareBB;
                        case "R" -> whiteRooks |= squareBB;
                        case "Q" -> whiteQueens |= squareBB;
                        case "K" -> whiteKing |= squareBB;
                        case "p" -> blackPawns |= squareBB;
                        case "n" -> blackKnights |= squareBB;
                        case "b" -> blackBishops |= squareBB;
                        case "r" -> blackRooks |= squareBB;
                        case "q" -> blackQueens |= squareBB;
                        case "k" -> blackKing |= squareBB;
                    }
                }
            }

            boolean whiteToMove = parseSideToMove(parts[1]);
            int castlingRights = parseCastlingRights(parts[2]);
            int enPassantFile = parseEnPassantFile(parts[3]);
            int fiftyMoveCounter = parts.length > 4 ? parseFiftyMoveCounter(parts[4]) : 0;
            // This implementation does not require the full move counter (parts[5]).

            Board board = new Board();
            board.setPawns(whitePawns | blackPawns);
            board.setKnights(whiteKnights | blackKnights);
            board.setBishops(whiteBishops | blackBishops);
            board.setRooks(whiteRooks | blackRooks);
            board.setQueens(whiteQueens | blackQueens);
            board.setKings(whiteKing | blackKing);
            board.setWhitePieces(whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing);
            board.setBlackPieces(blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing);
            board.setOccupied(board.getWhitePieces() | board.getBlackPieces());
            board.setPieceList(calculatePieceList(board));
            board.setWhiteToMove(whiteToMove);
            board.getGameState().setCastlingRights(castlingRights);
            board.getGameState().setEnPassantFile(enPassantFile);
            board.getGameState().setHalfMoveClock(fiftyMoveCounter);
            board.getGameState().setZobrist(Zobrist.generateKey(board));
            board.getGameState().setPawnZobrist(Zobrist.generatePawnKey(board));

            return board;

    }

    public static String toFEN(Board board) {
        try {
            StringBuilder sb = new StringBuilder();

            for (int rank = 7; rank >= 0; rank--) {
                int emptySquares = 0;
                for (int file = 0; file < 8; file++) {
                    int square = Board.squareIndex(rank, file);
                    Piece piece = board.pieceAt(square);
                    if (piece != null) {
                        if (emptySquares != 0) {
                            sb.append(emptySquares);
                            emptySquares = 0;
                        }
                        long squareBB = 1L << square;
                        boolean white = (board.getWhitePieces() & squareBB) != 0;
                        String pieceCode = Notation.PIECE_CODE_INDEX.get(piece);
                        if (white) pieceCode = pieceCode.toUpperCase();
                        sb.append(pieceCode);
                    } else {
                        emptySquares++;
                    }
                }
                if (emptySquares != 0) {
                    sb.append(emptySquares);
                }
                if (rank > 0) {
                    sb.append('/');
                }
            }

            String whiteToMove = toSideToMove(board.isWhiteToMove());
            sb.append(" ").append(whiteToMove);

            String castlingRights = toCastlingRights(board.getGameState().getCastlingRights());
            sb.append(" ").append(castlingRights);

            String enPassantSquare = toEnPassantSquare(board.getGameState().getEnPassantFile(), board.isWhiteToMove());
            sb.append(" ").append(enPassantSquare);

            String fiftyMoveCounter = toFiftyMoveCounter(board.getGameState().getHalfMoveClock());
            sb.append(" ").append(fiftyMoveCounter);

            String fullMoveNumber = toFullMoveCounter(board.getMoveHistory());
            sb.append(" ").append(fullMoveNumber);

            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(board.toString(), e);
        }
    }



    private static boolean parseSideToMove(String sideToMove) {
        return switch (sideToMove) {
            case "w" -> true;
            case "b" -> false;
            default -> throw new IllegalArgumentException("Invalid side to move! " + sideToMove);
        };
    }

    private static String toSideToMove(boolean sideToMove) {
        return sideToMove ? "w" : "b";
    }

    private static int parseCastlingRights(String castlingRights) {
        int castlingRightsMask = 0b0000;
        if (castlingRights.contains("K")) {
            castlingRightsMask |= 0b0001;
        }
        if (castlingRights.contains("Q")) {
            castlingRightsMask |= 0b0010;
        }
        if (castlingRights.contains("k")) {
            castlingRightsMask |= 0b0100;
        }
        if (castlingRights.contains("q")) {
            castlingRightsMask |= 0b1000;
        }
        return castlingRightsMask;
    }

    private static String toCastlingRights(int castlingRights) {
        if (castlingRights == 0b0000) {
            return "-";
        }
        String castlingRightsString = "";
        if ((castlingRights & 0b0001) != 0) {
            castlingRightsString += "K";
        }
        if ((castlingRights & 0b0010) != 0) {
            castlingRightsString += "Q";
        }
        if ((castlingRights & 0b0100) != 0) {
            castlingRightsString += "k";
        }
        if ((castlingRights & 0b1000) != 0) {
            castlingRightsString += "q";
        }
        return castlingRightsString;
    }

    private static int parseEnPassantFile(String enPassantSquare) {
        if (enPassantSquare.equals("-")) {
            return -1;
        }
        int square = Notation.fromNotation(enPassantSquare);
        return Board.file(square);
    }

    private static String toEnPassantSquare(int enPassantFile, boolean white) {
        int rank = white ? 2 : 5;
        if (enPassantFile == -1) {
            return "-";
        }
        return Notation.toNotation(Board.squareIndex(rank, enPassantFile));
    }

    private static int parseFiftyMoveCounter(String fiftyMoveCounter) {
        return Character.isDigit(fiftyMoveCounter.charAt(0)) ? Integer.parseInt(fiftyMoveCounter) : 0;
    }

    private static String toFiftyMoveCounter(int fiftyMoveCounter) {
        return Integer.toString(fiftyMoveCounter);
    }

    private static String toFullMoveCounter(Deque<Move> moveHistory) {
        int halfMoves = moveHistory.size();
        return Integer.toString(1 + (halfMoves / 2));
    }

    private static Stream<String> parseSquare(String square) {
        if (square.length() != 1) {
            throw new IllegalArgumentException("Illegal square char! " + square);
        }
        boolean isLetter = Character.isLetter(square.charAt(0));
        return isLetter ? Stream.of(square) : IntStream.range(0, Integer.parseInt(square)).mapToObj(i -> "x");
    }

    public static Piece[] calculatePieceList(Board board) {

        Piece[] pieceList = new Piece[64];
        for (int square = 0; square < 64; square++) {
            long squareMask = 1L << square;
            if ((squareMask & board.getPawns()) != 0)           pieceList[square] = Piece.PAWN;
            else if ((squareMask & board.getKnights()) != 0)    pieceList[square] = Piece.KNIGHT;
            else if ((squareMask & board.getBishops()) != 0)    pieceList[square] = Piece.BISHOP;
            else if ((squareMask & board.getRooks()) != 0)      pieceList[square] = Piece.ROOK;
            else if ((squareMask & board.getQueens()) != 0)     pieceList[square] = Piece.QUEEN;
            else if ((squareMask & board.getKings()) != 0)      pieceList[square] = Piece.KING;
        }
        return pieceList;

    }


}
