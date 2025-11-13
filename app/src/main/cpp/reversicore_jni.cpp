/*
 * reversicore_jni.cpp
 * JNI wrapper for ReversiCore C++ library
 * This allows Java/Android code to call the C++ game logic
 */

#include <jni.h>
#include <android/log.h>
#include "ReversiCore.h"

#define LOG_TAG "ReversiCoreJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace reversi;

// Helper to get the Board pointer from Java object
static Board* getNativeBoard(JNIEnv* env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    return reinterpret_cast<Board*>(env->GetLongField(thiz, fieldId));
}

// Helper to set the Board pointer in Java object
static void setNativeBoard(JNIEnv* env, jobject thiz, Board* board) {
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    env->SetLongField(thiz, fieldId, reinterpret_cast<jlong>(board));
}

extern "C" {

// Constructor
JNIEXPORT void JNICALL
Java_net_dynart_reversi_BoardNative_nativeInit(JNIEnv* env, jobject thiz) {
    Board* board = new Board();
    setNativeBoard(env, thiz, board);
    LOGI("Board created at %p", board);
}

// Destructor
JNIEXPORT void JNICALL
Java_net_dynart_reversi_BoardNative_nativeDestroy(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        LOGI("Destroying board at %p", board);
        delete board;
        setNativeBoard(env, thiz, nullptr);
    }
}

// Set start position
JNIEXPORT void JNICALL
Java_net_dynart_reversi_BoardNative_setStartPosition(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        board->setStartPosition();
    }
}

// Get piece at position
JNIEXPORT jint JNICALL
Java_net_dynart_reversi_BoardNative_getPiece(JNIEnv* env, jobject thiz, jint x, jint y) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        return board->getPiece(x, y);
    }
    return Board::PIECE_EMPTY;
}

// Check if current move is dark
JNIEXPORT jboolean JNICALL
Java_net_dynart_reversi_BoardNative_isDark(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        return board->isDark();
    }
    return false;
}

// Get game result
JNIEXPORT jint JNICALL
Java_net_dynart_reversi_BoardNative_getGameResult(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        return static_cast<jint>(board->getGameResult());
    }
    return static_cast<jint>(GameResult::UNKNOWN);
}

// Get dark pieces count
JNIEXPORT jint JNICALL
Java_net_dynart_reversi_BoardNative_getDarkPiecesCount(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        return board->getDarkPiecesCount();
    }
    return 0;
}

// Get light pieces count
JNIEXPORT jint JNICALL
Java_net_dynart_reversi_BoardNative_getLightPiecesCount(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (board) {
        return board->getLightPiecesCount();
    }
    return 0;
}

// Get available moves
JNIEXPORT jobjectArray JNICALL
Java_net_dynart_reversi_BoardNative_getMoves(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (!board) {
        return nullptr;
    }

    std::vector<Coord> moves = board->getMoves();

    // Find Coord class and constructor
    jclass coordClass = env->FindClass("net/dynart/reversi/Coord");
    jmethodID coordConstructor = env->GetMethodID(coordClass, "<init>", "(II)V");

    // Create array
    jobjectArray result = env->NewObjectArray(moves.size(), coordClass, nullptr);

    // Fill array
    for (size_t i = 0; i < moves.size(); i++) {
        jobject coord = env->NewObject(coordClass, coordConstructor, moves[i].x, moves[i].y);
        env->SetObjectArrayElement(result, i, coord);
        env->DeleteLocalRef(coord);
    }

    return result;
}

// Make a move
JNIEXPORT jboolean JNICALL
Java_net_dynart_reversi_BoardNative_makeMove(JNIEnv* env, jobject thiz, jobject move) {
    Board* board = getNativeBoard(env, thiz);
    if (!board) {
        return false;
    }

    // Extract x and y from Coord object
    jclass coordClass = env->GetObjectClass(move);
    jfieldID xField = env->GetFieldID(coordClass, "x", "I");
    jfieldID yField = env->GetFieldID(coordClass, "y", "I");

    int x = env->GetIntField(move, xField);
    int y = env->GetIntField(move, yField);

    Coord coord(x, y);
    return board->makeMove(coord);
}

// Calculate AI move
JNIEXPORT jobject JNICALL
Java_net_dynart_reversi_BoardNative_nativeRun(JNIEnv* env, jobject thiz) {
    Board* board = getNativeBoard(env, thiz);
    if (!board) {
        return nullptr;
    }

    Coord result = board->run();

    // Create Coord object
    jclass coordClass = env->FindClass("net/dynart/reversi/Coord");
    jmethodID coordConstructor = env->GetMethodID(coordClass, "<init>", "(II)V");

    return env->NewObject(coordClass, coordConstructor, result.x, result.y);
}

// Set max run depth (static method)
JNIEXPORT void JNICALL
Java_net_dynart_reversi_BoardNative_nativeSetMaxRunDepth(JNIEnv* env, jclass clazz, jint depth) {
    Board::maxRunDepth = depth;
    LOGI("Max run depth set to %d", depth);
}

} // extern "C"
