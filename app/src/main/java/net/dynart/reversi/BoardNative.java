package net.dynart.reversi;

/**
 * BoardNative - JNI wrapper for the C++ ReversiCore library
 * This class provides the same API as Board.java but uses native C++ code
 * for cross-platform compatibility (Android and iOS)
 */
public class BoardNative {

    // Load the native library
    static {
        System.loadLibrary("reversicore");
    }

    /**
     * Game result enumeration (matches C++ enum)
     */
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

    /**
     * Pointer to the native C++ Board object
     */
    private long nativePtr;

    /**
     * Constructor
     */
    public BoardNative() {
        nativeInit();
    }

    /**
     * Cleanup native resources
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            nativeDestroy();
        } finally {
            super.finalize();
        }
    }

    /**
     * Maximum depth of current calculation. It influences computer strength
     * Controls difficulty: 1-2 (easy), 3-5 (medium), 6-10 (hard)
     */
    public static int maxRunDepth = 5;

    // Native methods

    /**
     * Initialize the native Board object
     */
    private native void nativeInit();

    /**
     * Destroy the native Board object
     */
    private native void nativeDestroy();

    /**
     * Prepares start position on the board
     */
    public native void setStartPosition();

    /**
     * Returns a piece on the specified board position
     * @return PIECE_EMPTY, PIECE_DARK, or PIECE_LIGHT
     */
    public native int getPiece(int x, int y);

    /**
     * Determines piece color of current move
     */
    public native boolean isDark();

    /**
     * Returns state of the game
     */
    public native int getGameResult();

    /**
     * Returns count of dark pieces on the board
     */
    public native int getDarkPiecesCount();

    /**
     * Returns count of light pieces on the board
     */
    public native int getLightPiecesCount();

    /**
     * Returns possible moves in the current position
     */
    public native Coord[] getMoves();

    /**
     * Perform specified move
     * @return true if move was valid and performed, false otherwise
     */
    public native boolean makeMove(Coord move);

    /**
     * Calculates the best move in the current position and
     * returns the best move that the computer has found
     */
    public Coord run() {
        // Update native max depth before calculation
        nativeSetMaxRunDepth(maxRunDepth);
        return nativeRun();
    }

    /**
     * Native implementation of run()
     */
    private native Coord nativeRun();

    /**
     * Set maximum depth for AI calculations in native code
     */
    private static native void nativeSetMaxRunDepth(int depth);

    /**
     * Helper method to convert int game result to GameResult enum
     */
    public GameResult getGameResultEnum() {
        int result = getGameResult();
        if (result >= 0 && result < GameResult.values().length) {
            return GameResult.values()[result];
        }
        return GameResult.UNKNOWN;
    }
}
