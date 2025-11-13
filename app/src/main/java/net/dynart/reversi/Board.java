/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * Copyright  2008, 2010 Oracle and/or its affiliates.  All rights reserved.
 * Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *   * Neither the name of Oracle Corporation nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.dynart.reversi;

import java.util.*;

/**
 * @author Pavel Porvatov
 */
public class Board {
    public static enum GameResult {
        UNKNOWN,
        DRAW,
        DARK_WINS,
        LIGHT_WINS
    }

    /**
     * Cell value for an empty cell
     */
    public static final int PIECE_EMPTY = 0;

    /**
     * Cell value for a dark piece
     */
    public static final int PIECE_DARK = 1;

    /**
     * Cell value for a light piece
     */
    public static final int PIECE_LIGHT = -1;

    /*
     * Max result value for the intGetPosValue() method
     */
    private static final int intMaxPosValue = 10000;

    /**
     * Lose result value for the intGetPosValue() method
     */
    private static final int intLoseValue = 5000;

    /**
     * Max possible depth for computer calculations
     */
    private static final int intMaxDepth = 10;

    /**
     * All directions relative to a cell (down-lfet, down, down-righ, left, right etc)
     *
     * @see #data
     */
    private static final int[] directions = {-11, -10, -9, -1, 1, 9, 10, 11};

    /**
     * Indexes of corner cells and corner neighbours cells
     *
     * @see #data
     */
    private static final int[] intCorners = {
            coordToIndex(0, 0),
            coordToIndex(0, 1), coordToIndex(1, 1), coordToIndex(1, 0),
            coordToIndex(0, 7),
            coordToIndex(0, 6), coordToIndex(1, 6), coordToIndex(1, 7),
            coordToIndex(7, 0),
            coordToIndex(6, 0), coordToIndex(6, 1), coordToIndex(7, 1),
            coordToIndex(7, 7),
            coordToIndex(6, 7), coordToIndex(6, 6), coordToIndex(7, 6)};

    /**
     * Used for random number generation. Compatible with mobile platform
     */
    private static final Random RANDOM = new Random();

    /**
     * Data of board. The {@link #indexToCoord(int)} and
     * {@link #coordToIndex(int, int)} convert board coordinates
     * into array indexes and vice versa
     *
     * @see #PIECE_EMPTY
     * @see #PIECE_DARK
     * @see #PIECE_LIGHT
     */
    private final int[] data = new int[100];

    /**
     * Value of current piece
     *
     * @see #PIECE_DARK
     * @see #PIECE_LIGHT
     */
    private int movePiece;

    /**
     * Count of dark pieces on the board
     */
    private int intDarkPiecesCount;

    /**
     * Count of light pieces on the board
     */
    private int intLightPiecesCount;

    /**
     * Current depth calcaulation
     *
     * @see #run()
     */
    private int depth;

    /**
     * Maximum depth of current calcaulation. It influences on computer strength
     *
     * @see #run()
     */
    public static int maxRunDepth = 5;

    /**
     * States of board when computer calculates the best move.
     * We prepares all structures before computer calculations to avoid
     * performance problems.
     *
     * @see #run()
     */
    private final BoardState[] boardStates = new BoardState[intMaxDepth];

    /**
     * Current index in the {@link #posValueRandom} array
     */
    private int posValueRandomIndex;

    /**
     * Random noise for the {@link #intGetPosValue(boolean)} method
     */
    private final int[] posValueRandom = new int[100];

    public Board() {
        for (int i = 0; i < boardStates.length; i++) {
            boardStates[i] = new BoardState();
        }
        setStartPosition();
    }

    /**
     * Returns a piece on the specified board position
     *
     * @see #PIECE_EMPTY
     * @see #PIECE_DARK
     * @see #PIECE_LIGHT
     */
    public int getPiece(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            return PIECE_EMPTY;
        }
        return data[coordToIndex(x, y)];
    }

    /**
     * Prepares start position on the board
     */
    public void setStartPosition() {
        for (int i = 0; i < 100; i++) {
            data[i] = PIECE_EMPTY;
        }
        data[coordToIndex(3, 3)] = PIECE_LIGHT;
        data[coordToIndex(4, 4)] = PIECE_LIGHT;
        data[coordToIndex(3, 4)] = PIECE_DARK;
        data[coordToIndex(4, 3)] = PIECE_DARK;
        movePiece = PIECE_LIGHT;
        prepareMoves();
    }

    /**
     * Determines piece color of current move
     */
    public boolean isDark() {
        return movePiece == PIECE_DARK;
    }

    /**
     * Returns state of the game
     */
    public GameResult getGameResult() {
        if (boardStates[0].movesCount > 0) {
            return GameResult.UNKNOWN;
        }

        if (intDarkPiecesCount == intLightPiecesCount) {
            return GameResult.DRAW;
        }

        return intDarkPiecesCount > intLightPiecesCount ?
                GameResult.DARK_WINS : GameResult.LIGHT_WINS;
    }

    /**
     * Returns possible moves in the current position
     */
    public Coord[] getMoves() {
        BoardState boardState = boardStates[0];
        Coord[] result = new Coord[boardState.movesCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = indexToCoord(boardState.moves[i].move);
        }
        return result;
    }

    /**
     * Perform specified move
     */
    public boolean makeMove(Coord move) {
        BoardState boardState = boardStates[0];
        int index = coordToIndex(move.x, move.y);
        for (int i = 0; i < boardState.movesCount; i++) {
            MoveInfo moveInfo = boardState.moves[i];

            if (moveInfo.move == index) {
                intMakeMove(moveInfo);

                prepareMoves();

                return true;
            }
        }

        return false;
    }

    /**
     * Returns count of dark pieces on the board
     */
    public int getDarkPiecesCount() {
        return intDarkPiecesCount;
    }

    /**
     * Returns count of light pieces on the board
     */
    public int getLightPiecesCount() {
        return intLightPiecesCount;
    }

    /**
     * Prepares internal variables and finds possible moves
     * in current position
     */
    private void prepareMoves() {
        depth = 0;
        intDarkPiecesCount = 0;
        intLightPiecesCount = 0;
        for (int piece: data) {
            if (piece == PIECE_DARK) {
                intDarkPiecesCount++;
            }

            if (piece == PIECE_LIGHT) {
                intLightPiecesCount++;
            }
        }

        intFindMoves();
        if (boardStates[0].movesCount == 0) {
            movePiece = -movePiece;
            intFindMoves();
            if (boardStates[0].movesCount == 0) {
                movePiece = -movePiece;
            }
        }
    }

    /**
     * Calculates the best move in the current position and
     * returns the best move that the computer has found
     */
    public Coord run() {
        BoardState boardState = boardStates[0];

        if (boardState.movesCount == 1) {
            return indexToCoord(boardStates[0].moves[0].move);
        }

        ArrayList<Integer> bestMovesIndexes = new ArrayList<Integer>();

        // Prepare posValueRandom array
        for (int i = 0; i < posValueRandom.length; i++) {
            posValueRandom[i] = (int) (RANDOM.nextDouble() * 3) - 2;
        }

        int darkPiecesCount = intDarkPiecesCount;
        int lightPiecesCount = intLightPiecesCount;

        int bestValue = boardState.dark ? -intMaxPosValue : intMaxPosValue;

        for (int i = 0; i < boardState.movesCount; i++) {
            intMakeMove(boardState.moves[i]);

            int moveValue = intRekursPosValue();

            intUndo(boardState);

            intDarkPiecesCount = darkPiecesCount;
            intLightPiecesCount = lightPiecesCount;

            // Update result
            if (moveValue == bestValue) {
                bestMovesIndexes.add(i);
            } else if (boardState.dark ^ moveValue < bestValue) {
                bestValue = moveValue;

                bestMovesIndexes.clear();
                bestMovesIndexes.add(i);
            }
        }

        int bestMoveIndex = bestMovesIndexes.get(
                (int)(RANDOM.nextDouble() * bestMovesIndexes.size())
        );

        return indexToCoord(boardStates[0].moves[bestMoveIndex].move);
    }

    /**
     * Recursive function which calculates value of current position
     *
     * @see #run()
     */
    private int intRekursPosValue() {
        depth++;
        intFindMoves();
        BoardState boardState = boardStates[depth];

        if (boardState.movesCount == 0) {
            movePiece = -movePiece;
            intFindMoves();
            if (boardState.movesCount == 0) {
                // Game over
                depth--;

                return intGetPosValue(true);
            }
        }

        int darkPiecesCount = intDarkPiecesCount;
        int lightPiecesCount = intLightPiecesCount;
        int result = boardState.dark ? -intMaxPosValue : intMaxPosValue;

        for (int i = 0; i < boardState.movesCount; i++) {
            intMakeMove(boardState.moves[i]);
            int moveValue = depth < maxRunDepth ? intRekursPosValue() :
                    intGetPosValue(false);

            // Restore position
            intUndo(boardState);

            intDarkPiecesCount = darkPiecesCount;
            intLightPiecesCount = lightPiecesCount;

            // Update result
            if (boardState.dark) {
                if (moveValue > result) {
                    result = moveValue;
                }
            } else {
                if (moveValue < result) {
                    result = moveValue;
                }
            }
        }

        depth--;

        return result;
    }

    /**
     * Returns value of current position
     *
     * @see #run()
     */
    private int intGetPosValue(boolean gameOver) {
        int piecesCount = intDarkPiecesCount + intLightPiecesCount;
        if (gameOver || piecesCount == 64) {
            if (intDarkPiecesCount == intLightPiecesCount) {
                return 0;
            } else {
                // Winning position with more pieces shold have better value
                return intDarkPiecesCount > intLightPiecesCount ?
                        intLoseValue + intDarkPiecesCount - intLightPiecesCount:
                        -intLoseValue - intLightPiecesCount + intDarkPiecesCount;
            }
        }

        int result;

        if (piecesCount < 56) {
            int corners = 0;
            int neighbours = 0;

            // Corner cells are very well, price is 64
            // Corner neighbours are bad, price is -16
            for (int i = 0; i < intCorners.length; i += 4) {
                int piece = data[intCorners[i]];

                if (piece == PIECE_EMPTY) {
                    for (int j = 1; j < 4; j++) {
                        neighbours += data[intCorners[i + j]];
                    }
                } else {
                    corners += piece;
                }
            }

            // It's a good idea to have only few pieces
            result = (corners << 6) - (neighbours << 4) +
                    intLightPiecesCount - intDarkPiecesCount;
        } else {
            result = intDarkPiecesCount - intLightPiecesCount;
        }

        if (posValueRandomIndex >= 99) {
            posValueRandomIndex = 0;
        } else {
            posValueRandomIndex++;
        }

        return result + posValueRandom[posValueRandomIndex];
    }

    /**
     * Finds moves in the current position
     *
     * @see #run()
     */
    private void intFindMoves() {
        BoardState boardState = boardStates[depth];
        int opponentPiece = -movePiece;
        int currMovesCount = 0;
        int i = 11;
        while (i <= 88) {
            for (int y = 0; y < 8; y++) {
                if (data[i] == PIECE_EMPTY) {
                    MoveInfo moveInfo = null;

                    for (int dir: directions) {
                        if (data[i + dir] == opponentPiece) {
                            // May be "i" is a move. Find another piece
                            // on the opposite side
                            int pos = i + dir;

                            while (true) {
                                pos += dir;

                                int piece = data[pos];

                                if (piece == movePiece) {
                                    // "i" is a valid move
                                    if (moveInfo == null) {
                                        moveInfo = boardState.moves[currMovesCount];

                                        currMovesCount++;

                                        moveInfo.move = i;
                                        moveInfo.rotateDirectionsCount = 1;
                                        moveInfo.rotateDirections[0] = dir;
                                    } else {
                                        moveInfo.rotateDirections[moveInfo.rotateDirectionsCount] = dir;
                                        moveInfo.rotateDirectionsCount++;
                                    }
                                }

                                if (piece != opponentPiece) {
                                    break;
                                }
                            }
                        }
                    }
                }

                i++;
            }

            // Go to the next row
            i += 2;
        }

        boardState.movesCount = currMovesCount;

        // Store state
        boardState.dark = isDark();
        System.arraycopy(data, 0, boardState.data, 0, data.length);
    }

    /**
     * Perform specified move
     *
     * @see #run()
     */
    private void intMakeMove(MoveInfo moveInfo) {
        int opponentPiece = -movePiece;

        // Put a piece
        data[moveInfo.move] = movePiece;

        // Rotate enemy pieces
        int rotatedCount = 0;

        for (int i = 0; i < moveInfo.rotateDirectionsCount; i++) {
            int dir = moveInfo.rotateDirections[i];

            int pos = moveInfo.move;

            while (true) {
                pos += dir;

                if (data[pos] != opponentPiece) {
                    break;
                }

                data[pos] = movePiece;

                rotatedCount++;
            }
        }

        if (isDark()) {
            intDarkPiecesCount += 1 + rotatedCount;
            intLightPiecesCount -= rotatedCount;

            movePiece = PIECE_LIGHT;
        } else {
            intLightPiecesCount += 1 + rotatedCount;
            intDarkPiecesCount -= rotatedCount;

            movePiece = PIECE_DARK;
        }
    }

    /**
     * Restore position
     *
     * @see #run()
     */
    private void intUndo(BoardState boardState) {
        movePiece = boardState.dark ? PIECE_DARK : PIECE_LIGHT;

        System.arraycopy(boardState.data, 0, data, 0, data.length);
    }

    /**
     * Converts board coordinates into index of internal {@link #data} array
     */
    private static int coordToIndex(int x, int y) {
        return (y + 1) * 10 + x + 1;
    }

    /**
     * Converts index of internal {@link #data} array into board coordinates
     */
    private static Coord indexToCoord(int index) {
        return new Coord(index % 10 - 1, index / 10 - 1);
    }

    /**
     * Move information
     */
    private static class MoveInfo {
        public int move;

        public int rotateDirectionsCount;

        public final int[] rotateDirections = new int[directions.length];
    }

    /**
     * Board state
     */
    private static class BoardState {
        public boolean dark;

        public final int[] data = new int[100];

        public int movesCount;

        public final MoveInfo[] moves = new MoveInfo[64];

        public BoardState() {
            for (int i = 0; i < moves.length; i++) {
                moves[i] = new MoveInfo();
            }
        }
    }
}

