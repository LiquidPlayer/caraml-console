cmake_minimum_required(VERSION 3.4.1)

add_library(
        caraml-console.node
        SHARED
        src/main/cpp/caraml-console.cpp)

include_directories(
        src/../../../LiquidCore/deps/node-8.9.3/src
        src/../../../LiquidCore/deps/node-8.9.3/deps/v8/include
)

find_library(
        log-lib
        log)

add_library(
        node-lib
        SHARED
        IMPORTED
)

if(${ANDROID_ABI} STREQUAL armeabi-v7a)
    SET( ARCH_SIZE "32bit" )
elseif(${ANDROID_ABI} STREQUAL x86)
    SET( ARCH_SIZE "32bit" )
elseif(${ANDROID_ABI} STREQUAL x86_64)
    SET( ARCH_SIZE "64bit" )
elseif(${ANDROID_ABI} STREQUAL arm64-v8a)
    SET( ARCH_SIZE "64bit" )
endif()

set_target_properties(
        node-lib
        PROPERTIES IMPORTED_LOCATION
        ${PROJECT_SOURCE_DIR}/../../LiquidCore/deps/node-8.9.3/prebuilt/${ARCH_SIZE}/nosymbols/${ANDROID_ABI}/libnode.so
)

target_link_libraries( # Specifies the target library.
        caraml-console.node
        node-lib
        ${log-lib})