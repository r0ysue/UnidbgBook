package com.dta.r0dbg.linux.file;


import com.dta.r0dbg.memory.BaseStructure;
import com.dta.r0dbg.memory.Pointer;

import java.util.Arrays;
import java.util.List;

public class TimeSpec extends BaseStructure {
    public TimeSpec(Pointer pointer){
        super(pointer);
    }
    public int tv_sec; // unsigned long
    public int tv_nsec; // long

    @Override
    public List<String> getFieldOrder() {
        return Arrays.asList("tv_sec", "tv_nsec");
    }
}