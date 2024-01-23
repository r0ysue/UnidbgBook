//
// Created by root on 2/23/22.
//

#ifndef GOTCHECK_UTIL_H
#define GOTCHECK_UTIL_H

#include <link.h>

struct iterater_data{
    char *lib_name;
    dl_phdr_info **info;
};

int callback(struct dl_phdr_info *info,
                size_t size, void *data){

    struct iterater_data *data_ = (struct iterater_data *)data;
    if (strstr(info->dlpi_name, data_->lib_name) != nullptr){
        LOGD("%s:%lx", info->dlpi_name, info->dlpi_addr)
        dl_phdr_info* ptr = (dl_phdr_info*)malloc(sizeof(dl_phdr_info));
        memcpy(ptr, info, sizeof(dl_phdr_info));
        *data_->info = ptr;
        return 1;
    }
    return 0;
}

dl_phdr_info *get_lib_info(char *lib_name){
    struct iterater_data data;
    data.lib_name = lib_name;
    data.info = (dl_phdr_info **)malloc(sizeof(data.info));
    *data.info = nullptr;

    int ret = dl_iterate_phdr(callback, (void *)&data);
    if (ret){
        //found
        LOGD("found aim elf %s:%lx", (*data.info)->dlpi_name, (*data.info)->dlpi_addr);
        dl_phdr_info *ret = *data.info;
        free(data.info);
        return ret;
    }
    return nullptr;
}

#endif //GOTCHECK_UTIL_H
