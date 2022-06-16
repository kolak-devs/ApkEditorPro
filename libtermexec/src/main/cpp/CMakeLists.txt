cmake_minimum_required(VERSION 3.4.1)

#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS}  -DNDK_BUILD -std=gnu++11 -fexceptions -frtti")
#add_definitions("-DHAVE_CONFIG_H -I./apkeditor/libzip -fvisibility=hidden")

add_library(jackpal-termexec2 SHARED
            process.cpp)

# Include libraries needed for apkeditorpro lib
target_link_libraries(jackpal-termexec2
                      log
                      z)
