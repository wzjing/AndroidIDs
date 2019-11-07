cmake_minimum_required(VERSION 3.4.1)

add_library(ids SHARED main.cpp)

target_link_libraries(ids log)