/*
 * ReversiCore.cpp
 * Implementation of cross-platform Reversi/Othello game logic
 *
 * Based on Oracle's Reversi example code
 * Ported from Java to C++ for cross-platform use
 */

#include "ReversiCore.h"
#include <algorithm>
#include <cstring>
#include <chrono>

namespace reversi {

// Initialize static members
int Board::maxRunDepth = 5;

const int Board::DIRECTIONS[8] = {-11, -10, -9, -1, 1, 9, 10, 11};

const int Board::CORNERS[16] = {
    coordToIndex(0, 0),
    coordToIndex(0, 1), coordToIndex(1, 1), coordToIndex(1, 0),
    coordToIndex(0, 7),
    coordToIndex(0, 6), coordToIndex(1, 6), coordToIndex(1, 7),
    coordToIndex(7, 0),
    coordToIndex(6, 0), coordToIndex(6, 1), coordToIndex(7, 1),
    coordToIndex(7, 7),
    coordToIndex(6, 7), coordToIndex(6, 6), coordToIndex(7, 6)
};

Board::Board() : movePiece(PIECE_LIGHT), darkPiecesCount(0), lightPiecesCount(0),
                 depth(0), posValueRandomIndex(0) {
    // Initialize random number generator with current time
    auto seed = std::chrono::high_resolution_clock::now().time_since_epoch().count();
    randomGen.seed(static_cast<unsigned int>(seed));

    // Initialize board data
    std::memset(data, 0, sizeof(data));

    setStartPosition();
}

Board::~Board() {
}

void Board::setStartPosition() {
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

int Board::getPiece(int x, int y) const {
    if (x < 0 || x > 7 || y < 0 || y > 7) {
        return PIECE_EMPTY;
    }
    return data[coordToIndex(x, y)];
}

bool Board::isDark() const {
    return movePiece == PIECE_DARK;
}

GameResult Board::getGameResult() const {
    if (boardStates[0].movesCount > 0) {
        return GameResult::UNKNOWN;
    }

    if (darkPiecesCount == lightPiecesCount) {
        return GameResult::DRAW;
    }

    return darkPiecesCount > lightPiecesCount ?
           GameResult::DARK_WINS : GameResult::LIGHT_WINS;
}

int Board::getDarkPiecesCount() const {
    return darkPiecesCount;
}

int Board::getLightPiecesCount() const {
    return lightPiecesCount;
}

std::vector<Coord> Board::getMoves() const {
    const BoardState& state = boardStates[0];
    std::vector<Coord> result;
    result.reserve(state.movesCount);

    for (int i = 0; i < state.movesCount; i++) {
        result.push_back(indexToCoord(state.moves[i].move));
    }

    return result;
}

bool Board::makeMove(const Coord& move) {
    BoardState& state = boardStates[0];
    int index = coordToIndex(move.x, move.y);

    for (int i = 0; i < state.movesCount; i++) {
        const MoveInfo& moveInfo = state.moves[i];

        if (moveInfo.move == index) {
            makeMoveInternal(moveInfo);
            prepareMoves();
            return true;
        }
    }

    return false;
}

Coord Board::run() {
    BoardState& state = boardStates[0];

    if (state.movesCount == 1) {
        return indexToCoord(state.moves[0].move);
    }

    std::vector<int> bestMovesIndexes;

    // Prepare random values for position evaluation
    std::uniform_int_distribution<int> dist(-2, 0);
    for (int i = 0; i < 100; i++) {
        posValueRandom[i] = dist(randomGen);
    }

    int savedDarkCount = darkPiecesCount;
    int savedLightCount = lightPiecesCount;
    int bestValue = state.dark ? -MAX_POS_VALUE : MAX_POS_VALUE;

    for (int i = 0; i < state.movesCount; i++) {
        makeMoveInternal(state.moves[i]);

        int moveValue = rekursPosValue();

        undo(state);

        darkPiecesCount = savedDarkCount;
        lightPiecesCount = savedLightCount;

        // Update best moves
        if (moveValue == bestValue) {
            bestMovesIndexes.push_back(i);
        } else if (state.dark ? (moveValue > bestValue) : (moveValue < bestValue)) {
            bestValue = moveValue;
            bestMovesIndexes.clear();
            bestMovesIndexes.push_back(i);
        }
    }

    // Randomly select one of the best moves
    std::uniform_int_distribution<size_t> moveDist(0, bestMovesIndexes.size() - 1);
    int bestMoveIndex = bestMovesIndexes[moveDist(randomGen)];

    return indexToCoord(state.moves[bestMoveIndex].move);
}

// Private methods

int Board::coordToIndex(int x, int y) {
    return (y + 1) * 10 + x + 1;
}

Coord Board::indexToCoord(int index) {
    return Coord(index % 10 - 1, index / 10 - 1);
}

void Board::prepareMoves() {
    depth = 0;
    darkPiecesCount = 0;
    lightPiecesCount = 0;

    for (int i = 0; i < 100; i++) {
        if (data[i] == PIECE_DARK) {
            darkPiecesCount++;
        } else if (data[i] == PIECE_LIGHT) {
            lightPiecesCount++;
        }
    }

    findMoves();

    if (boardStates[0].movesCount == 0) {
        movePiece = -movePiece;
        findMoves();
        if (boardStates[0].movesCount == 0) {
            movePiece = -movePiece;
        }
    }
}

void Board::findMoves() {
    BoardState& state = boardStates[depth];
    int opponentPiece = -movePiece;
    int currMovesCount = 0;
    int i = 11;

    while (i <= 88) {
        for (int y = 0; y < 8; y++) {
            if (data[i] == PIECE_EMPTY) {
                MoveInfo* moveInfo = nullptr;

                for (int dirIdx = 0; dirIdx < 8; dirIdx++) {
                    int dir = DIRECTIONS[dirIdx];

                    if (data[i + dir] == opponentPiece) {
                        int pos = i + dir;

                        while (true) {
                            pos += dir;
                            int piece = data[pos];

                            if (piece == movePiece) {
                                // Valid move found
                                if (moveInfo == nullptr) {
                                    moveInfo = &state.moves[currMovesCount];
                                    currMovesCount++;
                                    moveInfo->move = i;
                                    moveInfo->rotateDirectionsCount = 1;
                                    moveInfo->rotateDirections[0] = dir;
                                } else {
                                    moveInfo->rotateDirections[moveInfo->rotateDirectionsCount] = dir;
                                    moveInfo->rotateDirectionsCount++;
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
        i += 2; // Skip to next row
    }

    state.movesCount = currMovesCount;
    state.dark = isDark();
    std::memcpy(state.data, data, sizeof(data));
}

void Board::makeMoveInternal(const MoveInfo& moveInfo) {
    int opponentPiece = -movePiece;

    // Place piece
    data[moveInfo.move] = movePiece;

    // Flip opponent pieces
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
        darkPiecesCount += 1 + rotatedCount;
        lightPiecesCount -= rotatedCount;
        movePiece = PIECE_LIGHT;
    } else {
        lightPiecesCount += 1 + rotatedCount;
        darkPiecesCount -= rotatedCount;
        movePiece = PIECE_DARK;
    }
}

void Board::undo(const BoardState& state) {
    movePiece = state.dark ? PIECE_DARK : PIECE_LIGHT;
    std::memcpy(data, state.data, sizeof(data));
}

int Board::rekursPosValue() {
    depth++;
    findMoves();
    BoardState& state = boardStates[depth];

    if (state.movesCount == 0) {
        movePiece = -movePiece;
        findMoves();
        if (state.movesCount == 0) {
            // Game over
            depth--;
            return getPosValue(true);
        }
    }

    int savedDarkCount = darkPiecesCount;
    int savedLightCount = lightPiecesCount;
    int result = state.dark ? -MAX_POS_VALUE : MAX_POS_VALUE;

    for (int i = 0; i < state.movesCount; i++) {
        makeMoveInternal(state.moves[i]);

        int moveValue = (depth < maxRunDepth) ?
                        rekursPosValue() : getPosValue(false);

        undo(state);

        darkPiecesCount = savedDarkCount;
        lightPiecesCount = savedLightCount;

        // Update result
        if (state.dark) {
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

int Board::getPosValue(bool gameOver) {
    int piecesCount = darkPiecesCount + lightPiecesCount;

    if (gameOver || piecesCount == 64) {
        if (darkPiecesCount == lightPiecesCount) {
            return 0;
        } else {
            return darkPiecesCount > lightPiecesCount ?
                   LOSE_VALUE + darkPiecesCount - lightPiecesCount :
                   -LOSE_VALUE - lightPiecesCount + darkPiecesCount;
        }
    }

    int result;

    if (piecesCount < 56) {
        int corners = 0;
        int neighbours = 0;

        // Evaluate corners and their neighbours
        for (int i = 0; i < 16; i += 4) {
            int piece = data[CORNERS[i]];

            if (piece == PIECE_EMPTY) {
                for (int j = 1; j < 4; j++) {
                    neighbours += data[CORNERS[i + j]];
                }
            } else {
                corners += piece;
            }
        }

        result = (corners << 6) - (neighbours << 4) +
                 lightPiecesCount - darkPiecesCount;
    } else {
        result = darkPiecesCount - lightPiecesCount;
    }

    // Add random noise
    if (posValueRandomIndex >= 99) {
        posValueRandomIndex = 0;
    } else {
        posValueRandomIndex++;
    }

    return result + posValueRandom[posValueRandomIndex];
}

} // namespace reversi
