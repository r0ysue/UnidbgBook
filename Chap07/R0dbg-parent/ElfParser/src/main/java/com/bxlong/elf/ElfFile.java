package com.bxlong.elf;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.bxlong.elf.ElfSegment.PT_DYNAMIC;
import static com.bxlong.elf.ElfSegment.PT_LOAD;

/**
 * An ELF (Executable and Linkable Format) file that can be a relocatable, executable, shared or core file.
 * <p>
 * Use one of the following methods to parse input to get an instance of this class:
 * <ul>
 *     <li>{@link #from(File)}</li>
 *     <li>{@link #from(byte[])}</li>
 *     <li>{@link #from(InputStream)}</li>
 *     <li>{@link #from(MappedByteBuffer)}</li>
 * </ul>
 * <p>
 * Resources about ELF files:
 * <ul>
 *  <li>http://man7.org/linux/man-pages/man5/elf.5.html</li>
 *  <li>http://en.wikipedia.org/wiki/Executable_and_Linkable_Format</li>
 *  <li>http://www.ibm.com/developerworks/library/l-dynamic-libraries/</li>
 *  <li>http://downloads.openwatcom.org/ftp/devel/docs/elf-64-gen.pdf</li>
 * </ul>
 */
public final class ElfFile {

    /**
     * Relocatable file type. A possible value of {@link #e_type}.
     */
    public static final int ET_REL = 1;
    /**
     * Executable file type. A possible value of {@link #e_type}.
     */
    public static final int ET_EXEC = 2;
    /**
     * Shared object file type. A possible value of {@link #e_type}.
     */
    public static final int ET_DYN = 3;
    /**
     * Core file file type. A possible value of {@link #e_type}.
     */
    public static final int ET_CORE = 4;

    /**
     * 32-bit objects.
     */
    public static final byte CLASS_32 = 1;
    /**
     * 64-bit objects.
     */
    public static final byte CLASS_64 = 2;

    /**
     * LSB data encoding.
     */
    public static final byte DATA_LSB = 1;
    /**
     * MSB data encoding.
     */
    public static final byte DATA_MSB = 2;

    /**
     * No architecture type.
     */
    public static final int ARCH_NONE = 0;
    /**
     * AT&amp;T architecture type.
     */
    public static final int ARCH_ATT = 1;
    /**
     * SPARC architecture type.
     */
    public static final int ARCH_SPARC = 2;
    /**
     * Intel 386 architecture type.
     */
    public static final int ARCH_i386 = 3;
    /**
     * Motorola 68000 architecture type.
     */
    public static final int ARCH_68k = 4;
    /**
     * Motorola 88000 architecture type.
     */
    public static final int ARCH_88k = 5;
    /**
     * Intel 860 architecture type.
     */
    public static final int ARCH_i860 = 7;
    /**
     * MIPS architecture type.
     */
    public static final int ARCH_MIPS = 8;
    public static final int ARCH_ARM = 0x28;
    public static final int ARCH_X86_64 = 0x3E;
    public static final int ARCH_AARCH64 = 0xB7;

    /**
     * Identifies the object file type. One of the ET_* constants in the class.
     */
    public final short e_type; // Elf32_Half
    /**
     * Byte identifying the size of objects, either {@link #CLASS_32} or {link {@value #CLASS_64} .
     */
    public final byte objectSize;

    /**
     * Returns a byte identifying the data encoding of the processor specific data. This byte will be either
     * DATA_INVALID, DATA_LSB or DATA_MSB.
     */
    public final byte encoding;

    public final byte elfVersion;
    public final byte abi;
    public final byte abiVersion;

    /**
     * The required architecture. One of the ARCH_* constants in the class.
     */
    public final short arch; // Elf32_Half
    /**
     * Version
     */
    public final int version; // Elf32_Word
    /**
     * Virtual address to which the system first transfers control. If there is no entry point for the file the value is
     * 0.
     */
    public final long entry_point; // Elf32_Addr
    /**
     * e_phoff. Program header table offset in bytes. If there is no program header table the value is 0.
     */
    public final long ph_offset; // Elf32_Off
    /**
     * e_shoff. Section header table offset in bytes. If there is no section header table the value is 0.
     */
    public final long sh_offset; // Elf32_Off
    /**
     * e_flags. Processor specific flags.
     */
    public final int flags; // Elf32_Word
    /**
     * e_ehsize. ELF header size in bytes.
     */
    public final short eh_size; // Elf32_Half
    /**
     * e_phentsize. Size of one entry in the file's program header table in bytes. All entries are the same size.
     */
    public final short ph_entry_size; // Elf32_Half

    public final short num_ph; // Elf32_Half
    /**
     * e_shentsize. Section header entry size in bytes - all entries are the same size.
     */
    public final short sh_entry_size; // Elf32_Half
    /**
     * e_shnum. Number of entries in the section header table, 0 if no entries.
     */
    public final short num_sh; // Elf32_Half

    /**
     * Elf{32,64}_Ehdr#e_shstrndx. Index into the section header table associated with the section name string table.
     * SH_UNDEF if there is no section name string table.
     */
    private final short sh_string_ndx; // Elf32_Half


    public static ElfFile from(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int totalRead = 0;
        byte[] buffer = new byte[8096];
        boolean firstRead = true;
        while (true) {
            int readNow = in.read(buffer, totalRead, buffer.length - totalRead);
            if (readNow == -1) {
                return from(baos.toByteArray());
            } else {
                if (firstRead) {
                    // Abort early.
                    if (readNow < 4) {
                        throw new ElfException("Bad first read");
                    } else {
                        if (!(0x7f == buffer[0] && 'E' == buffer[1] && 'L' == buffer[2] && 'F' == buffer[3]))
                            throw new ElfException("Bad magic number for file");
                    }
                    firstRead = false;
                }
                baos.write(buffer, 0, readNow);
            }
        }
    }

    public static ElfFile from(File file) throws ElfException, IOException {
        byte[] buffer = new byte[(int) file.length()];
        try {
            FileInputStream in = new FileInputStream(file);
            int totalRead = 0;
            while (totalRead < buffer.length) {
                int readNow = in.read(buffer, totalRead, buffer.length - totalRead);
                if (readNow == -1) {
                    throw new ElfException("Premature end of file");
                } else {
                    totalRead += readNow;
                }
            }
        } catch (Exception e) {

        }
        return from(buffer);
    }

    public static ElfFile from(byte[] buffer) throws ElfException, IOException {
        return new ElfFile(new BackingFile(new ByteArrayInputStream(buffer)));
    }

    public static ElfFile from(MappedByteBuffer mappedByteBuffer) throws ElfException, IOException {
        return new ElfFile(new BackingFile(mappedByteBuffer));
    }

    ElfParser parser = null;

    public final MemoizedObject<ElfSegment> programHeaders[];

    private ElfFile(BackingFile backingFile) throws ElfException {
        parser = new ElfParser(this, backingFile);

        byte[] ident = new byte[16];
        int bytesRead = parser.read(ident);
        if (bytesRead != ident.length)
            throw new ElfException("Error reading elf header (read " + bytesRead + "bytes - expected to read " + ident.length + "bytes)");

        if (!(0x7f == ident[0] && 'E' == ident[1] && 'L' == ident[2] && 'F' == ident[3]))
            throw new ElfException("Bad magic number for file");

        objectSize = ident[4];
        if (!(objectSize == CLASS_32 || objectSize == CLASS_64))
            throw new ElfException("Invalid object size class: " + objectSize);
        encoding = ident[5];
        if (!(encoding == DATA_LSB || encoding == DATA_MSB)) throw new ElfException("Invalid encoding: " + encoding);
        elfVersion = ident[6];
        if (elfVersion != 1) throw new ElfException("Invalid elf version: " + elfVersion);
        abi = ident[7]; // EI_OSABI, target operating system ABI
        abiVersion = ident[8]; // EI_ABIVERSION, ABI version. Linux kernel (after at least 2.6) has no definition of it.
        // ident[9-15] // EI_PAD, currently unused.

        e_type = parser.readShort();
        arch = parser.readShort();
        version = parser.readInt();
        entry_point = parser.readIntOrLong();
        ph_offset = parser.readIntOrLong();
        sh_offset = parser.readIntOrLong();
        flags = parser.readInt();
        eh_size = parser.readShort();
        ph_entry_size = parser.readShort();
        num_ph = parser.readShort();
        sh_entry_size = parser.readShort();
        num_sh = parser.readShort();
        if (num_sh == 0) {
            throw new ElfException("e_shnum is SHN_UNDEF(0), which is not supported yet"
                    + " (the actual number of section header table entries is contained in the sh_size field of the section header at index 0)");
        }
        sh_string_ndx = parser.readShort();
        if (sh_string_ndx == /* SHN_XINDEX= */0xffff) {
            throw new ElfException("e_shstrndx is SHN_XINDEX(0xffff), which is not supported yet"
                    + " (the actual index of the section name string table section is contained in the sh_link field of the section header at index 0)");
        }

        programHeaders = MemoizedObject.uncheckedArray(num_ph);
        for (int i = 0; i < num_ph; i++) {
            final long programHeaderOffset = ph_offset + (i * ph_entry_size);
            programHeaders[i] = new MemoizedObject<ElfSegment>() {
                @Override
                public ElfSegment computeValue() {
                    return new ElfSegment(parser, programHeaderOffset);
                }
            };
        }
    }

    public List<ElfSegment> loadSegments = new ArrayList<ElfSegment>();

    /**
     * 查找Load段
     *
     * @return
     */
    public List<ElfSegment> getLoadSegment() {
        if (loadSegments.size() == 0) {
            for (MemoizedObject<ElfSegment> m : programHeaders) {
                ElfSegment phdr = m.getValue();
                if (phdr.type == PT_LOAD) {
                    loadSegments.add(phdr);
                }
            }
        }
        return loadSegments;
    }

    public ElfSegment dynamicSegment = null;

    /**
     * 查找 Dynamic段
     *
     * @return
     */
    public ElfSegment getDynamicSegment() {
        if (dynamicSegment == null) {
            for (MemoizedObject<ElfSegment> m : programHeaders) {
                ElfSegment phdr = m.getValue();
                if (phdr.type == PT_DYNAMIC) {
                    dynamicSegment = phdr;
                    break;
                }
            }
        }
        return dynamicSegment;
    }

    /**
     * 读文件
     *
     * @param offset
     * @param size
     * @return
     */
    public byte[] getBytes(long offset, int size) {
        byte[] bs = new byte[size];
        parser.seek(offset);
        parser.read(bs);
        return bs;
    }

    public ElfSegment getProgramHeader(int i) {
        return programHeaders[i].getValue();
    }
}
