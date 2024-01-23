#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

int fib(int i){
    if(i == 0) return 0;
    if(i == 1) {
        return 0;
    }
    if (i == 2){
        return 1;
    }
    return fib(i-1) + fib(i-2);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_dta_lesson5_MainActivity_add(JNIEnv *env, jobject thiz, jint a, jint b) {
    //printf("hello world!");
    if(a < 0){
        a = -a;
    }
    if(b < 0){
        b = -b;
    }
    return fib(a+b);
}