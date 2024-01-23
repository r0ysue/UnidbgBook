package com.dta.r0dbg.linux.file;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.linux.socket.LocalUdpSocket;
import com.dta.r0dbg.memory.Pointer;
import com.dta.r0dbg.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import static com.dta.r0dbg.linux.UnixError.EBADF;
import static com.dta.r0dbg.linux.socket.ISocket.SOCK_DGRAM;

public class FileSystem implements IFileSystem {

    private IEmulate emulate;
    private String root_fs;
    private Map<Integer, LinuxIO> fileMap = new TreeMap<>();


    public FileSystem(IEmulate emulate) {
        this.emulate = emulate;
        root_fs = Objects.requireNonNull(FileSystem.class.getClassLoader().getResource("")).getPath() + "android";
        fileMap.put(0, null);
        fileMap.put(1, null);
        fileMap.put(2, null);
    }

    @Override
    public int fcntl(int fd, int cmd, long arg) {
        LinuxIO linuxIO = fileMap.get(fd);
        if (linuxIO == null) {
            emulate.getMemory().setErrno(EBADF);
            return -1;
        }

        return linuxIO.fcntl(emulate,cmd,arg);
    }

    @Override
    public int connect(int sockfd, Pointer addr, int addrlen) {
        LinuxIO file = fileMap.get(sockfd);
        if (file == null) {
            emulate.getMemory().setErrno(EBADF);
            return -1;
        }
        return file.connect(addr,addrlen);
    }

    @Override
    public int write(int fd, Pointer iov, int iovcnt) {
          LinuxIO file = fileMap.get(fd);
        if (file == null) {
            emulate.getMemory().setErrno(EBADF);
            return -1;
        }

        int count = 0;
        for (int i = 0; i < iovcnt; i++) {
            Pointer iov_base = iov.getPointer(i * 8L);
            int iov_len = iov.getInt(i * 8L + 4);
            byte[] data = iov_base.getByteArray(0, iov_len);
            count += file.write(data);
        }
        return count;
    }

    @Override
    public File getLDFile(String path) {
        path = "android/ld/" + path;
        File resourceFile = FileHelper.getResourceFile(FileSystem.class, path);
        if (resourceFile == null) {
            throw new FileException("can not found the " + path + " file.");
        }
        return resourceFile;
    }

    @Override
    public int open(String pathname, int flags) {
        String path = root_fs + pathname;
        File file = new File(path);

        boolean needCreate = false;
        if ((flags & IFileSystem.O_TRUNC) == IFileSystem.O_TRUNC) {
            file.deleteOnExit();
            needCreate = true;
        }
        if ((flags & IFileSystem.O_CREAT) == IFileSystem.O_CREAT) {
            needCreate = true;
        }

        if (!file.exists()) {
            if (needCreate) {
                try {
                    boolean created = file.createNewFile();
                    if (!created) {
                        return -1;
                    }
                } catch (IOException e) {
                    return -1;
                }
            } else {
                //set Errno
                return -1;
            }

        }

        LinuxFile linuxFile = new LinuxFile();
        linuxFile.io = file;
        linuxFile.canRead = true;
        if ((flags & IFileSystem.O_RDWR) == IFileSystem.O_RDWR) {
            linuxFile.canWrite = true;
        } else if ((flags & IFileSystem.O_WRONLY) == IFileSystem.O_WRONLY) {
            linuxFile.canRead = false;
            linuxFile.canWrite = false;
        }
        int unused_fd = get_unused_fd();
        fileMap.put(unused_fd, linuxFile);
        return unused_fd;
    }

    @Override
    public int read(int fd, Pointer buffer, int count) {
        LinuxIO file = getFile(fd);
        if (file == null) {
            emulate.getMemory().setErrno(EBADF);
            return -1;
        }
        if (!file.isCanRead()) {
            return -1;
        }
        return file.read(buffer, count);
    }

    @Override
    public int close(int fd) {
        LinuxIO remove = fileMap.remove(fd);
        if (remove == null) {
            return -1;
        }
        return 0;
    }

    public LinuxIO getFile(int fd) {
        return fileMap.get(fd);
    }

    @Override
    public int openSocket(int type) {
        switch (type) {
            case SOCK_DGRAM:
                int unused_fd = get_unused_fd();
                fileMap.put(unused_fd, new LocalUdpSocket());
                return unused_fd;
            default:
                throw new UnsupportedOperationException();
        }
        //return 0;
    }

    /**
     * 后期优化
     *
     * @return
     */
    private int get_unused_fd() {
        return fileMap.size();
    }

}
