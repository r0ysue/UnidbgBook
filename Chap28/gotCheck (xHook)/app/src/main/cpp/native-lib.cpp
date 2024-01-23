#include <jni.h>
#include <string>
#include "include/xhook.h"
#include "log.h"
#include "util.h"
#include "Linker.h"

using namespace std;

string (*old_say_hello)();

string say_hello(){
    string hello = "Hello from C++   ";
    return hello;
}

string new_say_hello(){
    string old = old_say_hello();
    string hello = "hook success!";
    return old.append(hello);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dta_gotcheck_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    int ret_register = xhook_register(".*libnative-lib\\.so$","_Z9say_hellov111", (void *)new_say_hello, (void **)&old_say_hello);
    if(ret_register){
        //error
        LOGD("hook say_hello error:%d", ret_register);
    } else{
        //success
        LOGD("hook say_hello success:%d", ret_register);
    }

    int ret_refresh = xhook_refresh(0);

    if(ret_refresh){
        //error
        LOGD("refresh say_hello error:%d", ret_refresh);
    } else{
        //success
        LOGD("refresh say_hello success:%d", ret_refresh);
    }

    string hello = say_hello();

    dl_phdr_info * info = get_lib_info("libnative-lib.so");
    if (info != nullptr){
        Linker linker(info->dlpi_addr, info->dlpi_name);
        if(linker.isGotHook("_Z9say_hellov")){
            hello.append(" DANGEROUS!!!!");
        } else{
            hello.append(" GOOD!!!!");
        }
    }
    return env->NewStringUTF(hello.c_str());
}