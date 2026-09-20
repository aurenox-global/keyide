cmake_minimum_required(VERSION 3.22.1)
project(keyide C CXX)

set(CMAKE_CXX_STANDARD 17)

# PTY nativo (terminal)
add_library(keyidepty SHARED pty.c)

# Puente a Node.js (nodejs-mobile)
add_library(keyidenode SHARED node_bridge.cpp)
add_library(libnode SHARED IMPORTED)
set_target_properties(libnode PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so)

find_library(log-lib log)

target_link_libraries(keyidepty ${log-lib})
target_link_libraries(keyidenode libnode ${log-lib})
