package com.bxlong.elf;

import java.io.IOException;

public class ElfSymbolStructure implements SymbolLocator{

    private final ElfParser parser;
    private final long offset;
    private final int entrySize;
    private final MemoizedObject<ElfStringTable> stringTable;
    private final MemoizedObject<HashTable> hashTable;

    ElfSymbolStructure(final ElfParser parser, long offset, int entrySize, MemoizedObject<ElfStringTable> stringTable, MemoizedObject<HashTable> hashTable) {
        this.parser = parser;
        this.offset = offset;
        this.entrySize = entrySize;
        this.stringTable = stringTable;
        this.hashTable = hashTable;
    }

    /** Returns the symbol at the specified index. The ELF symbol at index 0 is the undefined symbol. */

    public ElfSymbol getELFSymbol(int index) {
        return new ElfSymbol(parser, offset + (long) index * entrySize).setStringTable(stringTable.getValue());
    }

    public ElfSymbol getELFSymbolByAddr(long addr) throws IOException {
        if (hashTable == null) {
            throw new UnsupportedOperationException("hashTable is null");
        }
        return this.hashTable.getValue().findSymbolByAddress(this, addr);
    }
    private final int STB_GLOBAL = 1;
    private final int STB_WEAK = 2;
    public ElfSymbol getELFSymbolByName(String name,  boolean isRel) {
        if (hashTable == null) {
            return null;
        }
        ElfSymbol sym = hashTable.getValue().getSymbol(this, name);
        if (isRel && sym != null){
            switch (sym.getBinding()){
                case STB_GLOBAL:
                case STB_WEAK:
                    if (sym.isUndef()){
                        return null;
                    }
                    return sym;
            }
            return null;
        }else {
            return sym;
        }
    }

}
