package com.dta.r0dbg.android.module;

import com.bxlong.elf.ElfFile;

import java.util.List;

public class ElfModule {
    private String name;
    private long base;
    private long size;
    private long load_bias_;
    private int ref = 0;
    private boolean isLinked = false;
    private boolean isInit = false;
    private ElfFile elfFile;
    private List<ElfModule> needs;


    public List<ElfModule> getNeeds() {
        return needs;
    }

    public void setNeeds(List<ElfModule> needs) {
        this.needs = needs;
    }

    public ElfFile getElfFile() {
        return elfFile;
    }

    public void setElfFile(ElfFile elfFile) {
        this.elfFile = elfFile;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public boolean isLinked() {
        return isLinked;
    }

    public void setLinked(boolean linked) {
        isLinked = linked;
    }

    public int getRef() {
        return ref;
    }

    public void setRef(int ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getBase() {
        return base;
    }

    public void setBase(long base) {
        this.base = base;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLoad_bias_() {
        return load_bias_;
    }

    public void setLoad_bias_(long load_bias_) {
        this.load_bias_ = load_bias_;
    }
}
