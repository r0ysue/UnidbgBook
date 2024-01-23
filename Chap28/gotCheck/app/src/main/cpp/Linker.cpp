//
// Created by root on 2/23/22.
//

#include "Linker.h"


using namespace std;

Linker::Linker(ElfW(Addr) base, string name):base_(base), name_(move(name)) {
    if(ReadElfHeader() &&
            VerifyElfHeader() &&
            ReadProgramHeaders() &&
            ResolveProgramHeaders() &&
            ResolveDynamic()){
        inInited = true;
    }
//    LOGD("read_elf_header:%d", ReadElfHeader())
//    LOGD("verify_elf_header:%d", VerifyElfHeader())
//    LOGD("read_program_headers:%d",ReadProgramHeaders())
//    LOGD("resolve_program_headers:%d",ResolveProgramHeaders())
//    LOGD("resolve_dynamic:%d",ResolveDynamic())
}

Linker::~Linker() {

}

bool Linker::ReadElfHeader() {
    memcpy(&header_, (void *)base_, sizeof(header_));
    return true;
}

static int GetTargetElfMachine() {
#if defined(__arm__)
    return EM_ARM;
#elif defined(__aarch64__)
    return EM_AARCH64;
#elif defined(__i386__)
    return EM_386;
#elif defined(__mips__)
    return EM_MIPS;
#elif defined(__x86_64__)
  return EM_X86_64;
#endif
}

static const char* EM_to_string(int em) {
    if (em == EM_386) return "EM_386";
    if (em == EM_AARCH64) return "EM_AARCH64";
    if (em == EM_ARM) return "EM_ARM";
    if (em == EM_MIPS) return "EM_MIPS";
    if (em == EM_X86_64) return "EM_X86_64";
    return "EM_???";
}

size_t get_application_target_sdk_version(){
    char *m_szSdkVer = nullptr;
    __system_property_get("ro.build.version.sdk", m_szSdkVer);
    return strtol(m_szSdkVer, nullptr, 10);
}

bool Linker::VerifyElfHeader() {
    if (memcmp(header_.e_ident, ELFMAG, SELFMAG) != 0) {
        LOGE("%s has bad ELF magic", name_.c_str());
        return false;
    }

    // Try to give a clear diagnostic for ELF class mismatches, since they're
    // an easy mistake to make during the 32-bit/64-bit transition period.
    int elf_class = header_.e_ident[EI_CLASS];
#if defined(__LP64__)
    if (elf_class != ELFCLASS64) {
    if (elf_class == ELFCLASS32) {
      LOGE("%s is 32-bit instead of 64-bit", name_.c_str());
    } else {
      LOGE("%s has unknown ELF class: %d", name_.c_str(), elf_class);
    }
    return false;
  }
#else
    if (elf_class != ELFCLASS32) {
        if (elf_class == ELFCLASS64) {
            LOGE("%s is 64-bit instead of 32-bit", name_.c_str());
        } else {
            LOGE("%s has unknown ELF class: %d", name_.c_str(), elf_class);
        }
        return false;
    }
#endif

    if (header_.e_ident[EI_DATA] != ELFDATA2LSB) {
        LOGE("%s not little-endian: %d", name_.c_str(), header_.e_ident[EI_DATA]);
        return false;
    }

    if (header_.e_type != ET_DYN) {
        LOGE("%s has unexpected e_type: %d", name_.c_str(), header_.e_type);
        return false;
    }

    if (header_.e_version != EV_CURRENT) {
        LOGE("%s has unexpected e_version: %d", name_.c_str(), header_.e_version);
        return false;
    }

    if (header_.e_machine != GetTargetElfMachine()) {
        LOGE("%s has unexpected e_machine: %d (%s)", name_.c_str(), header_.e_machine,
               EM_to_string(header_.e_machine));
        return false;
    }

    if (header_.e_shentsize != sizeof(ElfW(Shdr))) {
        // Fail if app is targeting Android O or above
        if (get_application_target_sdk_version() >= __ANDROID_API_O__) {
            LOGE("%s has unsupported e_shentsize: 0x%x (expected 0x%zx)",
                           name_.c_str(), header_.e_shentsize, sizeof(ElfW(Shdr)));
            return false;
        }
        LOGD("%s has unsupported e_shentsize: 0x%x (expected 0x%zx)",
                name_.c_str(), header_.e_shentsize, sizeof(ElfW(Shdr)));

    }

    if (header_.e_shstrndx == 0) {
        // Fail if app is targeting Android O or above
        if (get_application_target_sdk_version() >= __ANDROID_API_O__) {
            LOGE("%s has invalid e_shstrndx", name_.c_str());
            return false;
        }

        LOGD("%s has invalid e_shstrndx", name_.c_str());
    }

    return true;
}

bool Linker::ReadProgramHeaders() {
    phdr_num_ = header_.e_phnum;

    // Like the kernel, we only accept program header tables that
    // are smaller than 64KiB.
    if (phdr_num_ < 1 || phdr_num_ > 65536/sizeof(ElfW(Phdr))) {
        LOGE("%s has invalid e_phnum: %zd", name_.c_str(), phdr_num_);
        return false;
    }

    // Boundary checks
    size_t size = phdr_num_ * sizeof(ElfW(Phdr));

    phdr_table_ = static_cast<ElfW(Phdr)* >((void *)(base_ + PAGE_OFFSET(header_.e_phoff)));
    return true;
}

bool Linker::ResolveProgramHeaders() {
    for (ElfW(Phdr) *phdr = phdr_table_; phdr < phdr_table_ + phdr_num_  ; phdr++) {
        if (phdr->p_type == PT_DYNAMIC){
            dynamic_ = static_cast<ElfW(Dyn)* >((void *)(base_ + phdr->p_vaddr));
            return true;
        }
    }
    return false;
}

bool Linker::ResolveDynamic() {
    for (ElfW(Dyn) *d = dynamic_; d->d_tag != DT_NULL; d++) {
        switch (d->d_tag) {
            case DT_STRTAB:
                strtab_ = reinterpret_cast<char*>((base_ + d->d_un.d_ptr));
                break;
            case DT_SYMTAB:
                symtab_ = reinterpret_cast<ElfW(Sym)*>(base_ + d->d_un.d_ptr);
                break;
            case DT_JMPREL:
#if defined(USE_RELA)
                plt_rela_ = reinterpret_cast<ElfW(Rela)*>(base_ + d->d_un.d_ptr);
#else
                plt_rel_ = reinterpret_cast<ElfW(Rel)*>(base_ + d->d_un.d_ptr);
#endif
                break;

            case DT_PLTRELSZ:
#if defined(USE_RELA)
                plt_rela_count_ = d->d_un.d_val / sizeof(ElfW(Rela));
#else
                plt_rel_count_ = d->d_un.d_val / sizeof(ElfW(Rel));
#endif
                break;
            case DT_RELA:
                rela_ = reinterpret_cast<ElfW(Rela)*>(base_ + d->d_un.d_ptr);
                break;

            case DT_RELASZ:
                rela_count_ = d->d_un.d_val / sizeof(ElfW(Rela));
                break;
        }
    }
    return true;
}

ElfW(Sym)* Linker::findSym(const string& symName){
    for (ElfW(Sym)* sym = symtab_; (void *)sym < (void *)strtab_; sym++) {
        if (symName == strtab_+sym->st_name){
            LOGD("findSym: %s", strtab_+sym->st_name);
            return sym;
        }
    }
    return nullptr;
}

bool Linker::isGotHook(const string& symName) {
    ElfW(Sym)* symbol = findSym(symName);
    if (symbol != nullptr){
        if (plt_rela_ != nullptr){
            for (ElfW(Rela)* rel = plt_rela_; rel <  plt_rela_+plt_rela_count_; rel++) {;
                ElfW(Word) sym = ELFW(R_SYM)(rel->r_info);
                if(sym > 0 && sym == (symbol - symtab_)){
                    LOGD("plt_rela: sym:%s, %d", strtab_ + (symtab_+sym)->st_name, sym)
                    void **got_addr = (void **)(base_+rel->r_offset);
                    void *except_addr = (void *)(base_ + symbol->st_value);
                    if (*got_addr != except_addr){
                        //hook
                        return true;
                    }
                }
            }
        }

        if (rela_ != nullptr){
            for (ElfW(Rela)* rel = rela_; rel <  rela_+rela_count_; rel++) {
                ElfW(Word) sym = ELFW(R_SYM)(rel->r_info);
                if(sym > 0 && sym == (symbol - symtab_)){
                    LOGD("rela: sym:%s, %d", strtab_ + (symtab_+sym)->st_name, sym)
                    void **got_addr = (void **)(base_+rel->r_offset);
                    void *except_addr = (void *)(base_ + symbol->st_value);
                    if (*got_addr != except_addr){
                        //hook
                        return true;
                    }
                }
            }
        }
    }
    return false;
}
