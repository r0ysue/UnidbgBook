package com.dta.r0dbg.memory;

import java.util.List;

public interface IStructure {
    /**
     * 获取字段排序
     * @return
     */
    List<String> getFieldOrder();


    /**
     * 写入内存
     */
    int write();
}
