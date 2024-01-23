//
// Created by root on 2/23/22.
//

#ifndef GOTCHECK_LOG_H
#define GOTCHECK_LOG_H

#include <android/log.h>

#define TAG "r0ysue"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__);

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__);

#endif //GOTCHECK_LOG_H
