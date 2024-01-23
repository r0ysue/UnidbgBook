#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_dta_anti_1methodid_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */, jobject person) {
    jclass Person = env->FindClass("com/dta/anti_methodid/Person");
    //jclass Zhangsan = env->FindClass("com/dta/anti_methodid/ZhangSan");
    jmethodID getName = env->GetMethodID(Person, "getName", "()Ljava/lang/String;");
    jobject name = env->CallObjectMethod(person, getName);
    return static_cast<jstring>(name);
}