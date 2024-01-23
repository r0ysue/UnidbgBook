package com.dta.r0dbg.memory;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.android.linker.Linker;
import com.dta.r0dbg.android.module.ElfModule;
import com.dta.r0dbg.bakend.arm.ARM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class Memory {
    private static final Logger logger = LoggerFactory.getLogger(Memory.class);

    private IEmulate emulate;
    private static long MMAP_BASE = 0x40000000;
    protected final Map<Long, MemoryMap> memoryMap = new TreeMap<>();
    private Linker linker;

    public Memory(IEmulate emulate){
        this.emulate = emulate;
        linker = new Linker(emulate);
    }

    public ElfModule loadLibrary(File elf, boolean isCallConstructors) {
        return linker.do_dlopen(elf, isCallConstructors);
    }

    public static final int MAP_FIXED = 0x10;
    public static final int MAP_ANONYMOUS = 0x20;

    public long mmap(long start, int length, int prot, int flags, int fd, int offset) {
        int alignedSize = (int) ARM.PAGE_END(length);
        //是否匿名映射
        boolean isAnonymous = ((flags & MAP_ANONYMOUS) != 0) || (start == 0 && fd <= 0 && offset == 0);
        if ((flags & MAP_FIXED) != 0 && isAnonymous) {
//            if (log.isDebugEnabled()) {
//                log.debug("mmap2 MAP_FIXED start=0x" + Long.toHexString(start) + ", length=" + length + ", prot=" + prot);
//            }
//
//            munmap(start, length);
            emulate.getBackend().mem_map(start, alignedSize, prot);
//            if (memoryMap.put(start, new MemoryMap(start, aligned, prot)) != null) {
//                log.warn("mmap2 replace exists memory map: start=" + Long.toHexString(start));
//            }
            return start;
        }
        if (isAnonymous) {
            long addr = allocateMapAddress(0, alignedSize);
            debug("mmap addr=0x%x, mmapBaseAddress=0x%x, start=0x%x, fd=%d, offset=0x%x, aligned=0x%x", addr, MMAP_BASE, start, fd, offset, alignedSize);
            emulate.getBackend().mem_map(addr, alignedSize, prot);
            if (memoryMap.put(addr, new MemoryMap(addr, alignedSize, prot)) != null) {
                debug("mmap replace exists memory map addr=0x%x", addr);
            }
            return addr;
        }

        throw new UnsupportedOperationException("can not resolve the [mmap]");
    }

    private long allocateMapAddress(long mask, long length) {
        Map.Entry<Long, MemoryMap> lastEntry = null;
        for (Map.Entry<Long, MemoryMap> entry : memoryMap.entrySet()) {
            if (lastEntry == null) {
                lastEntry = entry;
            } else {
                MemoryMap map = lastEntry.getValue();
                long mmapAddress = map.base + map.size;
                if (mmapAddress + length < entry.getKey() && (mmapAddress & mask) == 0) {
                    return mmapAddress;
                } else {
                    lastEntry = entry;
                }
            }
        }
        if (lastEntry != null) {
            MemoryMap map = lastEntry.getValue();
            long mmapAddress = map.base + map.size;
            if (mmapAddress < MMAP_BASE) {
                //log.debug("allocateMapAddress mmapBaseAddress=0x" + Long.toHexString(mmapBaseAddress) + ", mmapAddress=0x" + Long.toHexString(mmapAddress));
                setMMapBaseAddress(mmapAddress);
            }
        }

        long addr = MMAP_BASE;
        while ((addr & mask) != 0) {
            addr += ARM.getPageAlign();
        }
        setMMapBaseAddress(addr + length);
        return addr;
    }

    private void setMMapBaseAddress(long addr) {
        MMAP_BASE = addr;
    }


    private void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }

}
