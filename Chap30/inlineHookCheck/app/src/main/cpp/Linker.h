//
// Created by root on 2/23/22.
//

#ifndef GOTCHECK_LINKER_H
#define GOTCHECK_LINKER_H

#include <link.h>
#include <string>
#include "log.h"
#include <sys/system_properties.h>

// Returns the address of the page containing address 'x'.
#define PAGE_START(x)  ((x) & PAGE_MASK)

// Returns the offset of address 'x' in its page.
#define PAGE_OFFSET(x) ((x) & ~PAGE_MASK)

// Returns the address of the next page after address 'x', unless 'x' is
// itself at the start of a page.
#define PAGE_END(x)    PAGE_START((x) + (PAGE_SIZE-1))

#define USE_RELA 1

#if defined(__LP64__)
#define ELFW(what) ELF64_ ## what
#else
#define ELFW(what) ELF32_ ## what
#endif

#define ELF32_R_SYM(x) ((x) >> 8)
#define ELF32_R_TYPE(x) ((x) & 0xff)
#define ELF64_R_SYM(i) ((i) >> 32)
#define ELF64_R_TYPE(i) ((i) & 0xffffffff)

using namespace std;

class Linker {
public:
    Linker(ElfW(Addr) base, string name);

    ~Linker();

    bool isInlineHook(const string& symName);

private:
    ElfW(Addr) base_;
    string name_;
    ElfW(Ehdr) header_;
    size_t phdr_num_;
    ElfW(Phdr)* phdr_table_;
    ElfW(Dyn)* dynamic_;
    char* strtab_;
    ElfW(Sym)* symtab_;
    bool inInited = false;

#if defined(USE_RELA)
    ElfW(Rela)* plt_rela_ = nullptr;
    size_t plt_rela_count_ = 0;

    ElfW(Rela)* rela_ = nullptr;
    size_t rela_count_ = 0;
#else
    ElfW(Rel)* plt_rel_;
  size_t plt_rel_count_;

  ElfW(Rel)* rel_;
  size_t rel_count_;
#endif

    bool ReadElfHeader();

    bool VerifyElfHeader();

    bool ReadProgramHeaders();

    bool ResolveProgramHeaders();

    bool ResolveDynamic();

    ElfW(Sym)* findSym(const string& symName);

};


#endif //GOTCHECK_LINKER_H
