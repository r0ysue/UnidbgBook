#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_dta_anti_1findclass_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    jclass MainActivity = env->FindClass("com/dta/anti_findclass/MainActivity2");
    bool exception = env->ExceptionCheck();
    if (exception){
        hello.append("GOOD!");
        env->ExceptionClear();
    } else{
        hello.append("DANGEROUS!");
    }
    return env->NewStringUTF(hello.c_str());
}