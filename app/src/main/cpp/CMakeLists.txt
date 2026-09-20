cmake_minimum_required(VERSION 3.22.1)
project(keyidepty C)

add_library(keyidepty SHARED pty.c)

find_library(log-lib log)
target_link_libraries(keyidepty ${log-lib})
