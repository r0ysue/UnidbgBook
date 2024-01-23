#include <jni.h>
#include <string>


extern "C"
JNIEXPORT jstring JNICALL
Java_com_dta_lesson45_MainActivity_readEnv(JNIEnv *env, jobject thiz, jstring key) {
    // TODO: implement readEnv()
    char* key_ = const_cast<char *>(env->GetStringUTFChars(key, NULL));
    char* value = getenv(key_);
    env->ReleaseStringUTFChars(key, key_);
    return env->NewStringUTF(value);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_dta_lesson45_MainActivity_getSalt(JNIEnv *env, jobject thiz) {
    // TODO: implement getSalt()
    std::string salt("dta123");

    char* name = getenv("name");
    if (name == NULL){
        salt.append("find_exception!");
    } else{
        salt.append(name);
    }
    return env->NewStringUTF(salt.c_str());
}