package com.dta.r0dbg.android.emulate;

import com.dta.r0dbg.bakend.IBackend;
import com.dta.r0dbg.memory.Memory;

import java.io.File;

public interface IEmulate {

    int R_ARM_ABS32 = 2;
    int R_ARM_REL32 = 3;
    int R_ARM_COPY = 20;
    int R_ARM_GLOB_DAT = 21;
    int R_ARM_JUMP_SLOT = 22;
    int R_ARM_RELATIVE = 23;
    int R_ARM_IRELATIVE = 160;

    long LR = 0xffff0000L;

    void init();

    boolean is32Bit();

    boolean is64Bit();

    IBackend getBackend();

    Memory getMemory();

    File getSystemLibrary(String name);

    Number eFunc(long begin, long until);

    void traceCode(long begin, long end);

}
