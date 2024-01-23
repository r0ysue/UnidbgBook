package com.dta.r0dbg.linux;

import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.bakend.IBackend;
import com.dta.r0dbg.bakend.arm.ARM;
import com.dta.r0dbg.memory.Pointer;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unicorn.ArmConst;
import unicorn.InterruptHook;
import unicorn.Unicorn;

import static com.dta.r0dbg.bakend.arm.ARM.PAGE_END;
import static com.dta.r0dbg.bakend.arm.ARM.PAGE_START;
import static com.dta.r0dbg.linux.UnixError.EINVAL;
import static com.dta.r0dbg.linux.socket.ISocket.AF_LOCAL;

/**
 * http://androidxref.com/6.0.0_r5/xref/bionic/libc/kernel/uapi/asm-arm/asm/unistd.h
 */
public class SystemCallHandler implements InterruptHook {
    private static final int PR_GET_DUMPABLE = 3;
    private static final int PR_SET_DUMPABLE = 4;
    private static final int PR_SET_NAME = 15;
    private static final int PR_GET_NAME = 16;
    private static final int BIONIC_PR_SET_VMA = 0x53564d41;
    private static final int PR_SET_PTRACER = 0x59616d61;

    private static final Logger logger = LoggerFactory.getLogger(SystemCallHandler.class);

    private final long nanoTime = System.nanoTime();

    @Override
    public void hook(Unicorn u, int intno, Object user) {
        IEmulate emulate = (IEmulate) user;
        IBackend backend = emulate.getBackend();
        Long R7 = (Long) u.reg_read(ArmConst.UC_ARM_REG_R7);
        int NR = R7.intValue();
        debug(">>> System call occur, intno: %d, NR: %d", intno, NR);

        switch (NR) {
            case 3:
                //read
                backend.reg_write(ArmConst.UC_ARM_REG_R0, read(backend, emulate));
                break;
            case 4:
                //write
                backend.reg_write(ArmConst.UC_ARM_REG_R0, write(backend, emulate));
                break;
            case 6:
                //close
                backend.reg_write(ArmConst.UC_ARM_REG_R0, close(backend, emulate));
                break;
            case 20:
                //getpid
                backend.reg_write(ArmConst.UC_ARM_REG_R0, getpid(backend, emulate));
                break;
            case 45:
                //brk
                backend.reg_write(ArmConst.UC_ARM_REG_R0, brk(backend, emulate));
                break;
            case 91:
                //munmap
                backend.reg_write(ArmConst.UC_ARM_REG_R0, munmap(backend, emulate));
                break;
            case 125:
                //mprotect
                backend.reg_write(ArmConst.UC_ARM_REG_R0, mprotect(backend, emulate));
                break;
            case 146:
                //writev
                backend.reg_write(ArmConst.UC_ARM_REG_R0, writev(backend, emulate));
                break;
            case 172:
                //prctl
                backend.reg_write(ArmConst.UC_ARM_REG_R0, prctl(backend, emulate));
                break;
            case 192:
                //mmap2
                u.reg_write(ArmConst.UC_ARM_REG_R0, mmap2(backend, emulate));
                break;
            case 197:
                //fstat64
                u.reg_write(ArmConst.UC_ARM_REG_R0, fstat64(backend, emulate));
                break;
            case 199:
                //getresuid
                u.reg_write(ArmConst.UC_ARM_REG_R0, getresuid(backend, emulate));
                break;
            case 220:
                //madvise
                u.reg_write(ArmConst.UC_ARM_REG_R0, madvise());
                break;
            case 221:
                //fcntl64
                backend.reg_write(ArmConst.UC_ARM_REG_R0, fcntl64(backend, emulate));
                break;
            case 240:
                //futex
                u.reg_write(ArmConst.UC_ARM_REG_R0, futex(emulate));
                break;
            case 263:
                //gettime
                u.reg_write(ArmConst.UC_ARM_REG_R0, gettime(backend, emulate));
                break;
            case 281:
                //socket
                u.reg_write(ArmConst.UC_ARM_REG_R0, socket(backend, emulate));
                break;
            case 283:
                //connect
                u.reg_write(ArmConst.UC_ARM_REG_R0, connect(backend, emulate));
                break;
            case 322:
                //openat
                u.reg_write(ArmConst.UC_ARM_REG_R0, openat(backend, emulate));
                break;
            default:
                debug("System call occur, can not resolve, NR:%d", NR);
                Runtime.getRuntime().exit(-1);

        }
    }

    private int writev(IBackend backend, IEmulate emulate) {
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer iov = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        int iovcnt = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
//        if (log.isDebugEnabled()) {
//            for (int i = 0; i < iovcnt; i++) {
//                com.sun.jna.Pointer iov_base = iov.getPointer(i * 8L);
//                int iov_len = iov.getInt(i * 8L + 4);
//                byte[] data = iov_base.getByteArray(0, iov_len);
//                Inspector.inspect(data, "writev fd=" + fd + ", iov=" + iov + ", iov_base=" + iov_base);
//            }
//        }

        return emulate.getFileSystem().write(fd,iov,iovcnt);
    }

    private static final int CLOCK_REALTIME = 0;
    private static final int CLOCK_MONOTONIC = 1;
    private static final int CLOCK_THREAD_CPUTIME_ID = 3;
    private static final int CLOCK_MONOTONIC_RAW = 4;
    private static final int CLOCK_MONOTONIC_COARSE = 6;
    private static final int CLOCK_BOOTTIME = 7;

    private int gettime(IBackend backend, IEmulate emulate) {
        int clk_id = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer tp = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        long offset = clk_id == CLOCK_REALTIME ? System.currentTimeMillis() * 1000000L : System.nanoTime() - nanoTime;
        long tv_sec = offset / 1000000000L;
        long tv_nsec = offset % 1000000000L;
//        if (log.isDebugEnabled()) {
//            log.debug("clock_gettime clk_id=" + clk_id + ", tp=" + tp + ", offset=" + offset + ", tv_sec=" + tv_sec + ", tv_nsec=" + tv_nsec);
//        }
        switch (clk_id) {
            case CLOCK_REALTIME:
            case CLOCK_MONOTONIC:
            case CLOCK_MONOTONIC_RAW:
            case CLOCK_MONOTONIC_COARSE:
            case CLOCK_BOOTTIME:
                tp.setInt(0, (int) tv_sec);
                tp.setInt(4, (int) tv_nsec);
                return 0;
            case CLOCK_THREAD_CPUTIME_ID:
                tp.setInt(0, 0);
                tp.setInt(4, 1);
                return 0;
        }
        throw new UnsupportedOperationException("clk_id=" + clk_id);
    }

    private int getpid(IBackend backend, IEmulate emulate) {
        return emulate.getPid();
    }

    private int getresuid(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/getresuid.2.html
         * int getresuid(uid_t * ruid , uid_t * euid , uid_t * suid );
         * int getresgid(gid_t * rgid , gid_t * egid , gid_t * sgid );
         */
        return 0;
    }

    private int connect(IBackend backend, IEmulate emulate) {
        /**
         * int connect(int sockfd, const struct sockaddr *addr,
         *                    socklen_t addrlen);
         */
        int sockfd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer addr = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        int addrlen = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        return connect(emulate, sockfd, addr, addrlen);
    }

    private int connect(IEmulate emulate, int sockfd, Pointer addr, int addrlen) {
//        if (log.isDebugEnabled()) {
//            byte[] data = addr.getByteArray(0, addrlen);
//            Inspector.inspect(data, "connect sockfd=" + sockfd + ", addr=" + addr + ", addrlen=" + addrlen);
//        }

        return emulate.getFileSystem().connect(sockfd, addr, addrlen);

    }

    private int fcntl64(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/fcntl64.2.html
         * int fcntl(int fd, int cmd, ... / arg / );
         */
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        int cmd = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue();
        long arg = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        return fcntl(emulate, fd, cmd, arg);
    }

    private int fcntl(IEmulate emulate, int fd, int cmd, long arg) {
        debug("fcntl fd=" + fd + ", cmd=" + cmd + ", arg=" + arg);
        return emulate.getFileSystem().fcntl(fd, cmd, arg);

    }

    private int socket(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/socket.2.html
         * int socket(int domain, int type, int protocol);
         */
        int domain = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        int type = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue() & 0x7ffff;
        int protocol = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        switch (domain) {
            case AF_LOCAL:
                int fd = emulate.getFileSystem().openSocket(type);
                if (fd == -1) {
                    debug("create socket error, domain = %d, type = %d", domain, type);
                }
                return fd;
            default:
                throw new UnsupportedOperationException();
        }
        //return 0;
    }

    private int read(IBackend backend, IEmulate emulate) {
        /**
         *https://man7.org/linux/man-pages/man2/read.2.html
         *ssize_t read(int fd , void * buf , size_t count );
         */
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer buffer = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        int count = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        return read(emulate, fd, buffer, count);
    }

    protected final int read(IEmulate emulate, int fd, Pointer buffer, int count) {
        debug("read fd=" + fd + ", buffer=" + buffer + ", count=" + count + ", from=" + Long.toHexString(emulate.getBackend().reg_read(ArmConst.UC_ARM_REG_LR).intValue()));


        //FileIO file = fdMap.get(fd);
//        if (file == null) {
//            emulator.getMemory().setErrno(UnixEmulator.EBADF);
//            return -1;
//        }
        //int read = file.read(emulator.getBackend(), buffer, count);
//        if (verbose) {
//            System.out.printf("Read %d bytes from '%s'%n", read, file);
//        }
        return emulate.getFileSystem().read(fd, buffer, count);
    }


    private int munmap(IBackend backend, IEmulate emulate) {
        long timeInMillis = System.currentTimeMillis();
        long start = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue() & 0xffffffffL;
        int length = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue();
        if (start % emulate.getPageAlign() != 0) {
            emulate.getMemory().setErrno(EINVAL);
            return -1;
        }
        emulate.getMemory().munmap(start, length);
        debug("munmap start=0x" + Long.toHexString(start) + ", length=" + length + ", offset=" + (System.currentTimeMillis() - timeInMillis) + ", from=" + Pointer.register(emulate, ArmConst.UC_ARM_REG_LR));
        return 0;
    }


    private static final int FUTEX_WAIT = 0;
    private static final int FUTEX_WAKE = 1;

    private int futex(IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/futex.2.html
         * long syscall(SYS_futex, uint32_t *uaddr, int futex_op, uint32_t val,
         *                     const struct timespec *timeout);
         */
        Pointer uaddr = Pointer.register(emulate, ArmConst.UC_ARM_REG_R0);
        int futex_op = emulate.getBackend().reg_read(ArmConst.UC_ARM_REG_R1).intValue();
        int val = emulate.getBackend().reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        int old = uaddr.getInt(0);
        debug("futex uaddr=" + uaddr + ", _futexop=" + futex_op + ", op=" + (futex_op & 0x7f) + ", val=" + val + ", old=" + old);
        switch (futex_op & 0x7f) {
            case FUTEX_WAIT:
                if (old != val) {
                    throw new IllegalStateException("old=" + old + ", val=" + val);
                }
                Thread.yield();
                Pointer timeout = Pointer.register(emulate, ArmConst.UC_ARM_REG_R3);
                int mytype = val & 0xc000;
                int shared = val & 0x2000;
                debug("futex FUTEX_WAIT mytype=" + mytype + ", shared=" + shared + ", timeout=" + timeout + ", test=" + (mytype | shared));
                uaddr.setInt(0, mytype | shared);
                return 0;
            case FUTEX_WAKE:
                return 0;
            default:
                throw new AbstractMethodError();
        }
    }

    private int close(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/close.2.html
         * int close(int fd );
         */
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        int close = emulate.getFileSystem().close(fd);
        if (close == -1) {
            //emulate.getMemory().setErrno();
            debug("fd %d, close error", fd);
        }
        return 0;
    }

    private int write(IBackend backend, IEmulate emulate) {
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer str = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        int len = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        if (fd == 1) {
            System.out.println(str.getString(0));
        }
        return len;
    }

    private int fstat64(IBackend backend, IEmulate emulate) {


        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer stat = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        return fstat(emulate, fd, stat);
    }

    private int fstat(IBackend backend, IEmulate emulator) {
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer stat = Pointer.register(emulator, ArmConst.UC_ARM_REG_R1);
        return fstat(emulator, fd, stat);
    }

    protected int fstat(IEmulate emulator, int fd, Pointer statbuf) {
        return emulator.getFileSystem().getFile(fd).stat(statbuf);

    }

    private int openat(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/openat.2.html
         * int openat(int dirfd , const char * pathname , int flags );
         * int openat(int dirfd , const char * pathname , int flags , mode_t mode );
         */
        int dirfd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer pathname_p = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
        int flags = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        int mode = backend.reg_read(ArmConst.UC_ARM_REG_R3).intValue();
        String pathname = pathname_p.getString(0);

        pathname = FilenameUtils.normalize(pathname, true);
        debug("openat dirfd: %d, pathname: %s, flags: 0x%x, mode: 0x%x", dirfd, pathname, flags, mode);
        if ("/dev/pmsg0".equals(pathname)) {
            emulate.getMemory().setErrno(UnixError.ENOENT);
            return -1;
        }
        if (pathname.startsWith("/") || dirfd == -100) {
            int fd = open(emulate, pathname, flags);
            if (fd == -1) {
                debug("openat dirfd error: %d, pathname: %s, flags: 0x%x, mode: 0x%x", dirfd, pathname, flags, mode);
            }
            return fd;
        } else {
            //基于dir目录

            int fd = open(emulate, pathname, flags);
            if (fd == -1) {
                debug("openat dirfd error: %d, pathname: %s, flags: 0x%x, mode: 0x%x", dirfd, pathname, flags, mode);
            }
            throw new SysCallException("unsupported");
            //return fd;
        }
    }

    public final int open(IEmulate emulator, String pathname, int flags) {
        return emulator.getFileSystem().open(pathname, flags);
    }

    private Number prctl(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/prctl.2.html
         * int prctl(int option , unsigned long arg2 , unsigned long arg3 ,
         *                  unsigned long arg4 , unsigned long arg5 );
         */
        int option = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        long arg2 = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue() & 0xffffffffL;
//        if (log.isDebugEnabled()) {
//            log.debug("prctl option=0x" + Integer.toHexString(option) + ", arg2=0x" + Long.toHexString(arg2));
//        }
        switch (option) {
            case PR_GET_DUMPABLE:
            case PR_SET_DUMPABLE:
                return 0;
            case PR_SET_NAME: {
                Pointer threadName = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
                String name = threadName.getString(0);
                debug("prctl set thread name: [%s]", name);
                return 0;
            }
            case PR_GET_NAME: {
                String name = Thread.currentThread().getName();
                if (name.length() > 15) {
                    name = name.substring(0, 15);
                }
                debug("prctl get thread name: [%s]", name);
                Pointer buffer = Pointer.register(emulate, ArmConst.UC_ARM_REG_R1);
                buffer.setString(0, name);
                return 0;
            }
            case BIONIC_PR_SET_VMA:

                Pointer addr = Pointer.register(emulate, ArmConst.UC_ARM_REG_R2);
                int len = backend.reg_read(ArmConst.UC_ARM_REG_R3).intValue();
                Pointer pointer = Pointer.register(emulate, ArmConst.UC_ARM_REG_R4);
                debug("prctl set vma addr: 0x%x, len: %d, pointer: %s, name: %s", addr.peer, len, pointer.toString(), pointer.getString(0));
                return 0;
            case PR_SET_PTRACER:
                int pid = (int) arg2;
                debug("prctl set ptracer: %d", pid);
                return 0;
        }
        return 0;
    }

    private int brk(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/brk.2.html
         * int brk(void * addr );
         */
        long address = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue() & 0xffffffffL;
        return emulate.getMemory().brk(address);
    }

    private int mprotect(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/mprotect.2.html
         *  int mprotect(void * addr , size_t len , int prot );
         */
        long address = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue() & 0xffffffffL;
        int length = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue();
        int prot = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();

        long alignedAddress = PAGE_START(address);
        long alignedLength = PAGE_END(length);

        return emulate.getMemory().mprotect(alignedAddress, (int) alignedLength, prot);
    }

    private int madvise() {
        /**
         * https://man7.org/linux/man-pages/man2/madvise.2.html
         */
        return 0;
    }

    private int mmap2(IBackend backend, IEmulate emulate) {
        /**
         * https://man7.org/linux/man-pages/man2/mmap2.2.html
         * void *syscall(SYS_mmap2,
         * unsigned long addr,
         * unsigned long length,
         * unsigned long prot,
         * unsigned long flags,
         * unsigned long fd,
         * unsigned long pgoffset);
         */
        long addr = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue() & 0xffffffffL;
        int length = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue();
        int prot = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue();
        int flags = backend.reg_read(ArmConst.UC_ARM_REG_R3).intValue();
        int fd = backend.reg_read(ArmConst.UC_ARM_REG_R4).intValue();
        int pgoffset = backend.reg_read(ArmConst.UC_ARM_REG_R5).intValue() << ARM.MMAP2_SHIFT;

        debug("System call [mmap2] : addr: %d, length: %d, prot: %d, flags: %d, fd: %d, pgoffset: %d", addr, length, prot, flags, fd, pgoffset);

        long mmap = emulate.getMemory().mmap(addr, length, prot, flags, fd, pgoffset);
        return (int) mmap;
    }

    private void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }
}
