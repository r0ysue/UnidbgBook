package com.dta.r0dbg.linux.file;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.memory.Pointer;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;

public class LinuxFile implements LinuxIO{
    public File io;
    public boolean canRead;
    public boolean canWrite;
    private int pageAlign = 0x1000;
    private RandomAccessFile randomAccessFile;

    public boolean isCanRead(){
        return canRead;
    }

    @Override
    public int fcntl(IEmulate emulate, int cmd, long arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int connect(Pointer addr, int addrlen) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(byte[] data) {
        throw new UnsupportedOperationException();
    }

    public int read(Pointer buffer, int count) {
        checkFileOpen();
        byte[] data = new byte[count];
        try {
            int read = randomAccessFile.read(data);
            buffer.write(0, data, 0, read);
            return read;
        } catch (Exception e) {
            return -1;
        }
    }

    private void checkFileOpen() {
        if (randomAccessFile == null) {
            try {
                randomAccessFile = new RandomAccessFile(io, "r");
            } catch (Exception e) {
                throw new FileException(e.getMessage());
            }
        }
    }

    public int stat(Pointer stat_buf) {

        Stat stat = new Stat(stat_buf);
        int st_mode;
        if (IFileSystem.IO.STDOUT.equals(io.getName())) {
            st_mode = IFileSystem.IO.S_IFCHR | 0x777;
        } else if (Files.isSymbolicLink(io.toPath())) {
            st_mode = IFileSystem.IO.S_IFLNK;
        } else {
            st_mode = IFileSystem.IO.S_IFREG;
        }
        stat.st_dev = 1;
        stat.st_mode = st_mode;
        stat.st_uid = 6;
        stat.st_gid = 6;
        stat.st_size = io.length();
        stat.st_blksize = pageAlign;
        stat.st_ino = 1;
        stat.st_blocks = ((io.length() + pageAlign - 1) / pageAlign);
        stat.setLastModification(io.lastModified());

        stat.write();
        return 0;
    }
}
