package com.dta.r0dbg.memory;

import java.io.DataOutput;
import java.io.IOException;

public class MemoryMap {
    public final long base;
    public final long size;
    public final int prot;

    public MemoryMap(long base, long size, int prot) {
        this.base = base;
        this.size = size;
        this.prot = prot;
    }


    public void serialize(DataOutput out) throws IOException {
        out.writeLong(base);
        out.writeLong(size);
        out.writeInt(prot);
    }

    @Override
    public String toString() {
        return "MemoryMap{" +
                "base=0x" + Long.toHexString(base) +
                ", size=0x" + Long.toHexString(size) +
                ", prot=" + prot +
                '}';
    }
}
