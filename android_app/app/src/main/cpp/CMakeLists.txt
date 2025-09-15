cmake_minimum_required(VERSION 3.4.1)

include_directories(../../../../)

# NOTE: libstable-diffusion.so is a pre-built library.
# It is built in the root of the project by running the Android build script
# as described in Phase 1 of the development plan.
# The library is located in the `build-android/bin` directory.
add_library(stable-diffusion SHARED IMPORTED)
set_target_properties(stable-diffusion PROPERTIES IMPORTED_LOCATION
                      ../../../../build-android/bin/libstable-diffusion.so)

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp )

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # specified by the path variable.
                       ${log-lib}
                       jnigraphics
                       stable-diffusion )
