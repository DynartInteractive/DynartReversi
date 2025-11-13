/*
 * ReversiCore.h
 * Cross-platform C++ core for Reversi/Othello game logic
 * Shared between Android and iOS
 */

#ifndef REVERSICORE_H
#define REVERSICORE_H

#include <vector>
#include <random>

namespace reversi {

/**
 * Coordinate structure for board positions
 */
struct Coord {
    int x;
    int y;

    Coord() : x(0), y(0) {}
    Coord(int x, int y) : x(x), y(y) {}
};

/**
 * Game result enumeration
 */
enum class GameResult {
    UNKNOWN,
    DRAW,
    DARK_WINS,
    LIGHT_WINS
};

/**
 * Move information for internal AI calculations
 */
struct MoveInfo {
    int move;
    int rotateDirectionsCount;
    int rotateDirections[8];

    MoveInfo() : move(0), rotateDirectionsCount(0) {
        for (int i = 0; i < 8; i++) {
            rotateDirections[i] = 0;
        }
    }
};

/**
 * Board state for AI calculations
 */
struct BoardState {
    bool dark;
    int data[100];
    int movesCount;
    MoveInfo moves[64];

    BoardState() : dark(false), movesCount(0) {
        for (int i = 0; i < 100; i++) {
            data[i] = 0;
        }
    }
};

/**
 * Core Reversi/Othello game board and AI engine
 */
class Board {
public:
    // Piece constants
    static const int PIECE_EMPTY = 0;
    static const int PIECE_DARK = 1;
    static const int PIECE_LIGHT = -1;

    // Maximum depth for AI calculations
    static int maxRunDepth;

    Board();
    ~Board();

    // Game state methods
    void setStartPosition();
    int getPiece(int x, int y) const;
    bool isDark() const;
    GameResult getGameResult() const;

    // Piece counts
    int getDarkPiecesCount() const;
    int getLightPiecesCount() const;

    // Move methods
    std::vector<Coord> getMoves() const;
    bool makeMove(const Coord& move);
    Coord run();  // AI move calculation

private:
    // Constants
    static const int MAX_POS_VALUE = 10000;
    static const int LOSE_VALUE = 5000;
    static const int MAX_DEPTH = 10;
    static const int DIRECTIONS[8];
    static const int CORNERS[16];

    // Board data
    int data[100];
    int movePiece;
    int darkPiecesCount;
    int lightPiecesCount;

    // AI calculation state
    int depth;
    BoardState boardStates[MAX_DEPTH];

    // Random number generation
    std::mt19937 randomGen;
    int posValueRandomIndex;
    int posValueRandom[100];

    // Helper methods
    static int coordToIndex(int x, int y);
    static Coord indexToCoord(int index);

    void prepareMoves();
    void findMoves();
    void makeMoveInternal(const MoveInfo& moveInfo);
    void undo(const BoardState& state);
    int rekursPosValue();
    int getPosValue(bool gameOver);
};

} // namespace reversi

#endif // REVERSICORE_H