cmake \
-H./ \
-B./ninja \
-DANDROID_ABI=armeabi-v7a \
-DANDROID_PLATFORM=android-23 \
-DANDROID_NDK=/root/Android/Sdk/ndk/22.1.7171670 \
-DCMAKE_TOOLCHAIN_FILE=/root/Android/Sdk/ndk/22.1.7171670/build/cmake/android.toolchain.cmake \
-G Ninja
ninja -C./ninjad