package com.bxlong.elf;

import java.io.IOException;

public interface SymbolLocator {

    ElfSymbol getELFSymbol(int index);

    ElfSymbol getELFSymbolByName(String name, boolean isRel) throws IOException;

    ElfSymbol getELFSymbolByAddr(long addr) throws IOException;

}
