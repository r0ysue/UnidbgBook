#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_dta_bytealign_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    __asm(
    "add sp, sp, #1\n"
    "str x0, [sp]\n"
    "ldr x0, [sp]\n"
    "sub sp, sp, #1\n"
    );
    return env->NewStringUTF(hello.c_str());
}