# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DynartReversi is a cross-platform Reversi/Othello game with a C++ core and platform-specific UI layers. The game supports:
- Single player mode with three AI difficulty levels (easy, medium, hard)
- Two player mode (offline, local multiplayer on one device)
- Custom rendering using Android Canvas (Android) or UIKit/SwiftUI (iOS)
- **Cross-platform architecture**: Core game logic in C++, shared between Android and iOS

## Build and Development Commands

### Prerequisites for Android
- Android Studio with NDK installed
- CMake 3.22.1 or higher
- Android SDK (API level 24+)

### Building the project
```bash
./gradlew build
```

**Note**: This will compile both the C++ core library and the Java/Android code.

### Building debug APK
```bash
./gradlew assembleDebug
```

### Building release APK
```bash
./gradlew assembleRelease
```

### Running tests
```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### Installing to device
```bash
./gradlew installDebug
```

### Clean build
```bash
./gradlew clean
```

## Architecture

### Cross-Platform Core (C++)

The game logic has been extracted into platform-independent C++ code located in `app/src/main/cpp/`:

- **ReversiCore.h / ReversiCore.cpp**: Core game engine containing:
  - Board state representation using a 100-element array (10x10 with borders)
  - Move generation and validation
  - AI implementation using minimax algorithm with alpha-beta pruning
  - Position evaluation function (corners, edges, piece count)
  - Configurable AI strength via `Board::maxRunDepth` (1-10, default 5)
  - Random noise for varied gameplay

- **reversicore_jni.cpp**: JNI wrapper for Android, bridges Java and C++

- **CMakeLists.txt**: Build configuration for compiling C++ into native libraries

### Android Layer

#### Application Entry Point
- **Main.java**: The main Activity that manages scene transitions. It creates all scenes (Game, Menu, MenuDifficulty, MenuColor) on startup and switches between them using `setScene()`.

### Scene Management System
The app uses a custom scene-based architecture:
- **Scene.java**: Base class for all scenes. Extends Android View and handles:
  - Bitmap loading and caching (backgrounds, pieces, UI elements)
  - Custom rendering with Canvas
  - Touch event handling with coordinate transformation
  - Button management with layers (for modal dialogs)
  - Message box system (OK/Cancel dialogs)
  - Sound playback
  - Screen scaling/pixel ratio calculations for different screen sizes
  - Static board instance shared across all scenes

- **Game.java**: The gameplay scene that:
  - Renders the board with legal move highlights
  - Shows piece counts for both players
  - Handles player input for placing pieces
  - Manages AI turns (uses draw count to delay AI moves for visual effect)
  - Displays game result messages

- **Menu.java**: Main menu with buttons for Single/Multi player and sound toggle

- **MenuDifficulty.java**: Difficulty selection for single player (sets BoardNative.maxRunDepth)

- **MenuColor.java**: Color selection for single player (which side the player wants)

#### JNI Integration
- **BoardNative.java**: JNI wrapper that interfaces with the C++ core
  - Loads the native library (`libreverscore.so`)
  - Provides the same API as the original Board.java
  - Manages the lifecycle of the native C++ Board object
  - Translates between Java and C++ types (Coord, GameResult, etc.)

- **Board.java**: Original Java implementation (deprecated, can be removed)
  - Kept for reference but not used in production
  - The C++ version provides identical functionality with better performance

#### Supporting Classes
- **Button.java**: Custom button implementation with states (normal, pressed, selected)
- **Coord.java**: Simple x,y coordinate class

## Key Technical Details

### Coordinate Systems
The app uses multiple coordinate systems:
- Real screen pixels (device native resolution)
- Virtual coordinates (scaled to 800x480 logical resolution)
- Board coordinates (0-7 for each axis)
- Internal board array indices (calculated via coordToIndex/indexToCoord)

### AI Difficulty Levels
Controlled by `BoardNative.maxRunDepth`:
- Easy: 1-2 (< 100ms per move)
- Medium: 3-5 (100-500ms per move)
- Hard: 6-10 (500ms-2s per move)

### Asset Loading
Assets are loaded once and cached as static bitmaps in Scene.java to avoid repeated decoding.

### Native Library Build
The C++ code is compiled for multiple Android architectures:
- armeabi-v7a (32-bit ARM)
- arm64-v8a (64-bit ARM)
- x86 (32-bit x86 emulator)
- x86_64 (64-bit x86 emulator)

The Gradle build automatically invokes CMake to compile the native library.

## Important Notes

### Android-specific
- The app locks to landscape orientation
- Board state is shared statically across all scenes via Scene.board
- AI moves use a delay mechanism (draw_count) to make thinking visible
- Sound can be toggled but no volume control beyond system volume
- No persistence - game state is lost on app close

### Cross-platform Core
- The C++ core is platform-independent and can be used for iOS (see CROSS_PLATFORM_GUIDE.md)
- The Oracle license header in the original Board.java indicates it was derived from example code
- The C++ version provides better performance than the original Java implementation
- Memory management: BoardNative properly cleans up native objects in finalize()

### iOS Integration
For detailed instructions on integrating the C++ core with iOS, see `CROSS_PLATFORM_GUIDE.md`.
Key steps:
1. Add ReversiCore.h and ReversiCore.cpp to Xcode project
2. Create Objective-C++ bridge (ReversiCoreBridge.h/mm)
3. Build platform-specific UI with UIKit or SwiftUI
4. Set C++ Language Dialect to C++11 or higher in build settings
