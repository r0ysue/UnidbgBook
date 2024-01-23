#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_dta_lesson5_MainActivity_add(JNIEnv *env, jobject thiz, jint a, jint b) {
    if(a < 0){
        a = -a;
    }
    if(b < 0){
        b = -b;
    }
    return a + b;
}