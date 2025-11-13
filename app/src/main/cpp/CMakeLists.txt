# CMake configuration for ReversiCore
# This builds the shared C++ library for Android (and will be reused for iOS)

cmake_minimum_required(VERSION 3.22.1)

project("reversicore")

# C++11 is required for the core logic
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add the core C++ library
add_library(reversicore SHARED
    ReversiCore.cpp
    ReversiCore.h
    reversicore_jni.cpp
)

# Link with Android log library for JNI logging
find_library(log-lib log)

target_link_libraries(reversicore
    ${log-lib}
)
