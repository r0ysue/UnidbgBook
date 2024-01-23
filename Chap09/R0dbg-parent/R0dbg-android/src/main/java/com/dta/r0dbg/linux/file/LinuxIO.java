package com.dta.r0dbg.linux.file;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.memory.Pointer;

public interface LinuxIO {
    int read(Pointer buffer, int count);

    int stat(Pointer stat_buf);

    boolean isCanRead();

    int fcntl(IEmulate emulate, int cmd, long arg);

    int connect(Pointer addr, int addrlen);

    int write(byte[] data);
}
