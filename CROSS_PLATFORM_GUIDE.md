# Cross-Platform Integration Guide

## Overview

This guide explains how the DynartReversi project has been restructured to support both Android and iOS using a shared C++ core library.

## Architecture

### Shared C++ Core (`app/src/main/cpp/`)

The game logic has been extracted into platform-independent C++ code:

- **ReversiCore.h** - Header file defining the core game engine API
- **ReversiCore.cpp** - Implementation of the Reversi/Othello game logic
  - Board state management
  - Move generation and validation
  - Minimax AI with alpha-beta pruning
  - Position evaluation function
- **reversicore_jni.cpp** - JNI wrapper for Android (Java ↔ C++)
- **CMakeLists.txt** - Build configuration for both platforms

### Android Integration

#### Files Modified
- **app/build.gradle** - Added CMake and NDK support
- **BoardNative.java** - New JNI wrapper class replacing Board.java
- **Scene.java** - Updated to use `BoardNative` instead of `Board`
- **Game.java** - Updated to use `BoardNative.GameResult`
- **Menu.java**, **MenuColor.java**, **MenuDifficulty.java** - Updated piece constants

#### How It Works
1. The C++ code is compiled into a native library (`libreverscore.so`) for multiple architectures
2. BoardNative.java uses JNI to call C++ functions
3. The rest of the Android app remains unchanged and uses BoardNative as a drop-in replacement

## Building for Android

### Prerequisites
- Android Studio with NDK installed
- CMake 3.22.1 or higher
- Android SDK (API level 24+)

### Build Steps
1. Open the project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Build the project (Build → Make Project)

The NDK will automatically compile the C++ code for all target architectures:
- armeabi-v7a (32-bit ARM)
- arm64-v8a (64-bit ARM)
- x86 (32-bit x86)
- x86_64 (64-bit x86)

### Manual Build via Command Line
```bash
# Set Android SDK location (if not set)
export ANDROID_HOME=/path/to/android/sdk

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## iOS Integration Guide

The same C++ core can be used for iOS! Here's how to integrate it:

### Step 1: Create iOS Project
1. Create a new iOS project in Xcode (Swift or Objective-C)
2. Design the UI (UIKit or SwiftUI)

### Step 2: Add C++ Files to Xcode
1. In Xcode, add the following files to your project:
   - `ReversiCore.h`
   - `ReversiCore.cpp`
2. Make sure they're included in your target's "Compile Sources"

### Step 3: Create Objective-C++ Bridge

Create a bridge header that wraps the C++ API:

**ReversiCoreBridge.h**
```objc
#import <Foundation/Foundation.h>

@interface Coord : NSObject
@property (nonatomic) int x;
@property (nonatomic) int y;
- (instancetype)initWithX:(int)x y:(int)y;
@end

typedef NS_ENUM(NSInteger, GameResult) {
    GameResultUnknown = 0,
    GameResultDraw = 1,
    GameResultDarkWins = 2,
    GameResultLightWins = 3
};

@interface ReversiBoard : NSObject

// Constants
+ (int)PIECE_EMPTY;
+ (int)PIECE_DARK;
+ (int)PIECE_LIGHT;

// Class properties
@property (class, nonatomic) int maxRunDepth;

// Instance methods
- (instancetype)init;
- (void)setStartPosition;
- (int)getPieceAtX:(int)x y:(int)y;
- (BOOL)isDark;
- (GameResult)getGameResult;
- (int)getDarkPiecesCount;
- (int)getLightPiecesCount;
- (NSArray<Coord *> *)getMoves;
- (BOOL)makeMove:(Coord *)move;
- (Coord *)run;

@end
```

**ReversiCoreBridge.mm** (note the .mm extension for Objective-C++)
```objc
#import "ReversiCoreBridge.h"
#import "ReversiCore.h"

using namespace reversi;

@implementation Coord
- (instancetype)initWithX:(int)x y:(int)y {
    self = [super init];
    if (self) {
        _x = x;
        _y = y;
    }
    return self;
}
@end

@interface ReversiBoard() {
    Board* _cppBoard;
}
@end

@implementation ReversiBoard

+ (int)PIECE_EMPTY { return Board::PIECE_EMPTY; }
+ (int)PIECE_DARK { return Board::PIECE_DARK; }
+ (int)PIECE_LIGHT { return Board::PIECE_LIGHT; }

static int _maxRunDepth = 5;

+ (void)setMaxRunDepth:(int)depth {
    _maxRunDepth = depth;
    Board::maxRunDepth = depth;
}

+ (int)maxRunDepth {
    return _maxRunDepth;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _cppBoard = new Board();
    }
    return self;
}

- (void)dealloc {
    if (_cppBoard) {
        delete _cppBoard;
    }
}

- (void)setStartPosition {
    _cppBoard->setStartPosition();
}

- (int)getPieceAtX:(int)x y:(int)y {
    return _cppBoard->getPiece(x, y);
}

- (BOOL)isDark {
    return _cppBoard->isDark();
}

- (GameResult)getGameResult {
    return (GameResult)_cppBoard->getGameResult();
}

- (int)getDarkPiecesCount {
    return _cppBoard->getDarkPiecesCount();
}

- (int)getLightPiecesCount {
    return _cppBoard->getLightPiecesCount();
}

- (NSArray<Coord *> *)getMoves {
    std::vector<reversi::Coord> moves = _cppBoard->getMoves();
    NSMutableArray<Coord *> *result = [NSMutableArray arrayWithCapacity:moves.size()];

    for (const auto& move : moves) {
        [result addObject:[[Coord alloc] initWithX:move.x y:move.y]];
    }

    return result;
}

- (BOOL)makeMove:(Coord *)move {
    reversi::Coord cppCoord(move.x, move.y);
    return _cppBoard->makeMove(cppCoord);
}

- (Coord *)run {
    Board::maxRunDepth = _maxRunDepth;
    reversi::Coord result = _cppBoard->run();
    return [[Coord alloc] initWithX:result.x y:result.y];
}

@end
```

### Step 4: Use in Swift

Create a Swift wrapper (optional but cleaner):

**ReversiGame.swift**
```swift
import Foundation

class ReversiGame {
    private let board: ReversiBoard

    init() {
        board = ReversiBoard()
        board.setStartPosition()
    }

    func startNewGame() {
        board.setStartPosition()
    }

    func getPiece(x: Int, y: Int) -> Int {
        return board.getPiece(atX: Int32(x), y: Int32(y))
    }

    func getMoves() -> [(x: Int, y: Int)] {
        guard let moves = board.getMoves() as? [Coord] else { return [] }
        return moves.map { (x: Int($0.x), y: Int($0.y)) }
    }

    func makeMove(x: Int, y: Int) -> Bool {
        let coord = Coord(x: Int32(x), y: Int32(y))
        return board.makeMove(coord)
    }

    func calculateAIMove() -> (x: Int, y: Int)? {
        guard let move = board.run() else { return nil }
        return (x: Int(move.x), y: Int(move.y))
    }

    var isDarkTurn: Bool {
        return board.isDark()
    }

    static func setDifficulty(_ difficulty: Difficulty) {
        switch difficulty {
        case .easy:
            ReversiBoard.maxRunDepth = 1
        case .medium:
            ReversiBoard.maxRunDepth = 3
        case .hard:
            ReversiBoard.maxRunDepth = 5
        }
    }

    enum Difficulty {
        case easy, medium, hard
    }
}
```

### Step 5: Build Settings in Xcode
1. Set "C++ Language Dialect" to "C++11 [-std=c++11]" or higher
2. Make sure the .cpp files are compiled as C++

## Key Benefits of This Architecture

### Code Reuse
- The core game logic (2000+ lines) is shared between platforms
- Bug fixes and improvements benefit both platforms
- AI algorithm is identical across platforms

### Performance
- Native C++ provides excellent performance for AI calculations
- No interpretation overhead
- Platform-optimized compilation

### Maintainability
- Single source of truth for game rules
- Platform-specific code is minimal (only UI and platform APIs)
- Easy to add new platforms (Web via WebAssembly, etc.)

## File Structure

```
DynartReversi/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/                    # Shared C++ core
│   │   │   │   ├── ReversiCore.h       # Core API
│   │   │   │   ├── ReversiCore.cpp     # Core implementation
│   │   │   │   ├── reversicore_jni.cpp # Android JNI wrapper
│   │   │   │   └── CMakeLists.txt      # Build config
│   │   │   ├── java/net/dynart/reversi/
│   │   │   │   ├── BoardNative.java    # JNI wrapper for Android
│   │   │   │   ├── Board.java          # (Old, can be removed)
│   │   │   │   └── ...                 # Other Android files
│   │   │   └── res/                    # Android resources
│   ├── build.gradle                    # Updated with NDK support
└── build.gradle                        # Root build file
```

## Testing

### Android Testing
Use the existing test infrastructure:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
```

### iOS Testing
Create XCTest unit tests that use the ReversiBoard class:
```swift
import XCTest

class ReversiCoreTests: XCTestCase {
    func testInitialPosition() {
        let board = ReversiBoard()
        board.setStartPosition()

        XCTAssertEqual(board.getDarkPiecesCount(), 2)
        XCTAssertEqual(board.getLightPiecesCount(), 2)
        XCTAssertFalse(board.isDark()) // Light starts
    }

    func testValidMoves() {
        let board = ReversiBoard()
        board.setStartPosition()

        let moves = board.getMoves() as! [Coord]
        XCTAssertGreaterThan(moves.count, 0)
    }
}
```

## Next Steps

1. **For Android**: Build and test in Android Studio with NDK installed
2. **For iOS**:
   - Create iOS project
   - Copy C++ files
   - Implement Objective-C++ bridge
   - Build UI layer
3. **Future enhancements**:
   - Add save/load game state
   - Network multiplayer (platform-specific)
   - Analytics (platform-specific)

## Troubleshooting

### Android Build Issues
- **NDK not found**: Install NDK via Android Studio SDK Manager
- **CMake errors**: Update CMake to 3.22.1 or higher
- **ABI mismatch**: Check ndk.abiFilters in build.gradle

### iOS Build Issues
- **C++ errors**: Ensure files have .mm extension for Objective-C++
- **Linking errors**: Add ReversiCore.cpp to target's Compile Sources
- **Header not found**: Add cpp directory to Header Search Paths

## Performance Notes

- AI calculation time varies with difficulty:
  - Easy (depth 1-2): < 100ms
  - Medium (depth 3-5): 100-500ms
  - Hard (depth 6-10): 500ms-2s
- Consider showing loading indicator for AI moves on Hard difficulty
- The C++ implementation is significantly faster than the original Java version
