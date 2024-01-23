package com.dta.r0dbg.memory;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.android.linker.Linker;
import com.dta.r0dbg.android.module.ElfModule;
import com.dta.r0dbg.bakend.arm.ARM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;
import unicorn.Unicorn;
import unicorn.UnicornConst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static com.dta.r0dbg.android.emulate.IEmulate.LR;

public final class Memory {
    private static final Logger logger = LoggerFactory.getLogger(Memory.class);
    private final IEmulate emulate;
    private final Linker linker;
    final long STACK_BASE = 0xc0000000L;
    int STACK_SIZE_OF_PAGE = 256; // 1024k
    private static long MMAP_BASE = 0x40000000;
    private boolean isInit = false;

    protected final Map<Long, MemoryMap> memoryMap = new TreeMap<>();

    public void init() {
        if (isInit)
            return;
        // map返回地址的内存
        emulate.getBackend().mem_map(LR, 0x1000, Unicorn.UC_PROT_ALL);

        //设置SP
        emulate.getBackend().reg_write(ArmConst.UC_ARM_REG_SP, STACK_BASE);
        long stackSize = STACK_SIZE_OF_PAGE * 4096;
        emulate.getBackend().mem_map(STACK_BASE - stackSize, stackSize, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);
        isInit = true;
    }

    public String getElfModuleNameByAddress(long address) {
        return linker.getElfModuleNameByAddress(address);
    }

    public ElfModule getElfModuleByAddress(long address) {
        return linker.getElfModuleByAddress(address);
    }

    public Memory(IEmulate emulate) {
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
//            return start;
        }
        if (isAnonymous) {
            long addr = allocateMapAddress(0, alignedSize);

            debug("mmap addr=0x%x, mmapBaseAddress=0x%x, start=0x%x, fd=%d, offset=0x%x, aligned=0x%x", addr, MMAP_BASE, start, fd, offset, alignedSize);
            try {
                emulate.getBackend().mem_map(addr, alignedSize, prot);
            }catch (Exception e){
                e.printStackTrace();
            }
            if (memoryMap.put(addr, new MemoryMap(addr, alignedSize, prot)) != null) {
                debug("mmap replace exists memory map addr=0x%x", addr);
            }
            return addr;
        }

        throw new UnsupportedOperationException("can not resolve the [mmap]");
    }

    public final int munmap(long start, int length) {
        int aligned = (int) ARM.alignSize(length, emulate.getPageAlign());
        emulate.getBackend().mem_unmap(start, aligned);
        MemoryMap removed = memoryMap.remove(start);

        if (removed == null) {
            MemoryMap segment = null;
            for (Map.Entry<Long, MemoryMap> entry : memoryMap.entrySet()) {
                MemoryMap map = entry.getValue();
                if (start > entry.getKey() && start < map.base + map.size) {
                    segment = entry.getValue();
                    break;
                }
            }
            if (segment == null || segment.size < aligned) {
                throw new IllegalStateException("munmap aligned=0x" + Long.toHexString(aligned) + ", start=0x" + Long.toHexString(start));
            }
            if (start + aligned < segment.base + segment.size) {
                long newSize = segment.base + segment.size - start - aligned;
//                if (log.isDebugEnabled()) {
//                    log.debug("munmap aligned=0x" + Long.toHexString(aligned) + ", start=0x" + Long.toHexString(start) + ", base=0x" + Long.toHexString(start + aligned) + ", size=" + newSize);
//                }
//                if (memoryMap.put(start + aligned, new MemoryMap(start + aligned, (int) newSize, segment.prot)) != null) {
//                    log.warn("munmap replace exists memory map addr=0x" + Long.toHexString(start + aligned));
//                }
            }
//            if (memoryMap.put(segment.base, new MemoryMap(segment.base, (int) (start - segment.base), segment.prot)) == null) {
//                log.warn("munmap replace failed warning: addr=0x" + Long.toHexString(segment.base));
//            }
//            if (log.isDebugEnabled()) {
//                log.debug("munmap aligned=0x" + Long.toHexString(aligned) + ", start=0x" + Long.toHexString(start) + ", base=0x" + Long.toHexString(segment.base) + ", size=" + (start - segment.base));
//            }
            return segment.prot;
        }

        if(removed.size != aligned) {
            if (aligned >= removed.size) {
//                if (log.isDebugEnabled()) {
//                    log.debug("munmap removed=0x" + Long.toHexString(removed.size) + ", aligned=0x" + Long.toHexString(aligned) + ", start=0x" + Long.toHexString(start));
//                }
                long address = start + removed.size;
                long size = aligned - removed.size;
                while (size != 0) {
                    MemoryMap remove = memoryMap.remove(address);
                    if (removed.prot != remove.prot) {
                        throw new IllegalStateException();
                    }
                    address += remove.size;
                    size -= remove.size;
                }
                return removed.prot;
            }

            if (memoryMap.put(start + aligned, new MemoryMap(start + aligned, removed.size - aligned, removed.prot)) != null) {
                debug("munmap replace exists memory map addr=0x" + Long.toHexString(start + aligned));
            }
//            if (log.isDebugEnabled()) {
//                log.debug("munmap removed=0x" + Long.toHexString(removed.size) + ", aligned=0x" + Long.toHexString(aligned) + ", base=0x" + Long.toHexString(start + aligned) + ", size=" + (removed.size - aligned));
//            }
            return removed.prot;
        }

//        if (log.isDebugEnabled()) {
//            log.debug("munmap aligned=0x" + Long.toHexString(aligned) + ", start=0x" + Long.toHexString(start) + ", base=0x" + Long.toHexString(removed.base) + ", size=" + removed.size);
//        }
        if (memoryMap.isEmpty()) {
            setMMapBaseAddress(MMAP_BASE);
        }
        return removed.prot;
    }



    public final int mprotect(long address, int length, int prot) {
//        if (address % ARMEmulator.PAGE_ALIGN != 0) {
//            setErrno(UnixEmulator.EINVAL);
//            return -1;
//        }
        emulate.getBackend().mem_protect(address, length, prot);
        return 0;
    }

    private static final long HEAP_BASE = 0x8048000;
    private long brk;

    public int brk(long address) {
        if (address == 0) {
            this.brk = HEAP_BASE;
            return (int) this.brk;
        }

        if (ARM.PAGE_OFFSET(address) != 0) {
            throw new UnsupportedOperationException();
        }

        if (address > brk) {
            emulate.getBackend().mem_map(brk, address - brk, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);
            this.brk = address;
        } else if(address < brk) {
            emulate.getBackend().mem_unmap(address, brk - address);
            this.brk = address;
        }

        return (int) this.brk;
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
    long sp = STACK_BASE;

    public Pointer allocateStack(int size){
        setStackPoint(sp - size);
        Pointer pointer = Pointer.point(emulate, sp);
        return pointer.setSize(size);
    }


    public final Pointer writeStackString(String str) {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        return writeStackBytes(Arrays.copyOf(data, data.length + 1));
    }


    public final Pointer writeStackBytes(byte[] data) {
        int size = ARM.alignSize(data.length);
        Pointer pointer = allocateStack(size);
        assert pointer != null;
        pointer.write(0, data, 0, data.length);
        return pointer;
    }

    public void setStackPoint(long sp){
        this.sp = sp;
        if (emulate.is32Bit()) {
            emulate.getBackend().reg_write(ArmConst.UC_ARM_REG_SP,sp);
        } else {
            emulate.getBackend().reg_write(Arm64Const.UC_ARM64_REG_SP, sp);
        }
    }

    public long getStackPoint(){
        return sp;
    }

    public void setErrno(int errno){
        emulate.getErrnoPointer().setInt(0,errno);
    }




    private void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }
}
