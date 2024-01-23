package com.dta.r0dbg.linux.file;

import com.dta.r0dbg.memory.BaseStructure;
import com.dta.r0dbg.memory.Pointer;

import java.util.Arrays;
import java.util.List;

public class Stat extends BaseStructure {
    public Stat(Pointer pointer){
        super(pointer);
    }
    public long st_dev;
    public long st_ino;
    public int st_mode;
    public int st_nlink;
    public int st_uid;
    public int st_gid;
    public long st_rdev;
    public long st_size;
    public int st_blksize;
    public long st_blocks;
    public byte[] __pad0 = new byte[]{1,1,1,1};
    public int __st_ino;
    public byte[] __pad3 = new byte[]{2,2,2,2};

    public TimeSpec st_atim;
    public TimeSpec st_mtim;
    public TimeSpec st_ctim;

    @Override
    public List<String> getFieldOrder() {
        return Arrays.asList("st_dev", "__pad0", "__st_ino", "st_mode", "st_nlink", "st_uid", "st_gid", "st_rdev", "__pad3",
                "st_size", "st_blksize", "st_blocks", "st_atim", "st_mtim", "st_ctim", "st_ino");
    }
    public void setSt_atim(long st_atim, long tv_nsec) {
        this.st_atim.tv_sec = (int) (st_atim / 1000L);
        this.st_atim.tv_nsec = (int) ((st_atim % 1000) * 1000000L + (tv_nsec % 1000000L));
    }
    public void setSt_mtim(long st_mtim, long tv_nsec) {
        this.st_mtim.tv_sec = (int) (st_mtim / 1000L);
        this.st_mtim.tv_nsec = (int) ((st_mtim % 1000) * 1000000L + tv_nsec % 1000000L);
    }
    public void setSt_ctim(long st_ctim, long tv_nsec) {
        this.st_ctim.tv_sec = (int) (st_ctim / 1000L);
        this.st_ctim.tv_nsec = (int) ((st_ctim % 1000) * 1000000L + tv_nsec % 1000000L);
    }


    public void setLastModification(long lastModified) {
        setSt_atim(lastModified, 0L);
        setSt_mtim(lastModified, 0L);
        setSt_ctim(lastModified, 0L);
    }


}