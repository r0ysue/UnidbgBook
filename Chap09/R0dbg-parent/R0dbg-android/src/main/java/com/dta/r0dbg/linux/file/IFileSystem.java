package com.dta.r0dbg.linux.file;

import com.dta.r0dbg.memory.Pointer;

import java.io.File;

public interface IFileSystem {
    int O_RDONLY = 0;
    int O_WRONLY = 1;
    int O_RDWR = 2;

    int O_APPEND = 1024;
    int O_ASYNC = 8192;
    int O_CREAT = 64;
    int O_DIRECT = 16384;
    int O_EXCL = 128;
    int O_LARGEFILE = 32768;
    int O_NOCTTY = 256;
    int O_NOFOLLOW = 131072;
    int O_NONBLOCK = 2048;
    int O_SYNC = 4096;
    int O_TRUNC = 512;

    int fcntl(int fd, int cmd, long arg);

    int connect(int sockfd, Pointer addr, int addrlen);

    int write(int fd, Pointer iov, int iovcnt);

    public interface IO {
        String STDIN = "stdin";
        int FD_STDIN = 0;

        String STDOUT = "stdout";
        int FD_STDOUT = 1;

        String STDERR = "stderr";
        int FD_STDERR = 2;

        int S_IFREG = 0x8000;   // regular file
        int S_IFDIR = 0x4000;   // directory
        int S_IFCHR = 0x2000;   // character device
        int S_IFLNK = 0xa000;   // symbolic link
        int S_IFSOCK = 0xc000;   // socket

        int AT_FDCWD = -100;
    }

    /**
     * 获取系统库
     *
     * @param path
     * @return
     */
    File getLDFile(String path);

    /**
     * 打开文件
     *
     * @param pathname
     * @param flags
     * @return
     */
    int open(String pathname, int flags);

    /**
     * 读取文件内容
     *
     * @param fd
     * @param buffer
     * @param count
     * @return
     */
    int read(int fd, Pointer buffer, int count);

    int close(int fd);

    /**
     * 根据fd获取文件
     *
     * @param fd
     * @return
     */
    LinuxIO getFile(int fd);

    /**
     * openSocket
     *
     * @param type
     */

    int openSocket(int type);
}
