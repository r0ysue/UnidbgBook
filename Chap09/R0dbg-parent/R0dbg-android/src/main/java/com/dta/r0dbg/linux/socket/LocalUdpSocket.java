package com.dta.r0dbg.linux.socket;


import com.dta.r0dbg.android.emulate.IEmulate;
import com.dta.r0dbg.linux.file.LinuxIO;
import com.dta.r0dbg.memory.Pointer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.dta.r0dbg.linux.file.IFileSystem.*;
import static com.dta.r0dbg.linux.socket.ISocket.AF_LOCAL;
import static com.dta.r0dbg.linux.socket.ISocket.F_SETFL;

public class LocalUdpSocket implements LinuxIO {
    private UdpHandler handler;
    int oflags;
    @Override
    public int read(Pointer buffer, int count) {
        return 0;
    }

    @Override
    public int stat(Pointer stat_buf) {
        return 0;
    }

    @Override
    public boolean isCanRead() {
        return false;
    }

    @Override
    public int fcntl(IEmulate emulate, int cmd, long arg) {
        switch (cmd){
            case F_SETFL:
                if ((O_APPEND & arg) != 0) {
                    oflags |= O_APPEND;
                }
                if ((O_RDWR & arg) != 0) {
                    oflags |= O_RDWR;
                }
                if ((O_NONBLOCK & arg) != 0) {
                    oflags |= O_NONBLOCK;
                }
                return 0;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public int connect(Pointer addr, int addrlen) {
        short sa_family = addr.getShort(0);
        if (sa_family != AF_LOCAL){
            throw new UnsupportedOperationException("sa_family = "+sa_family);
        }
        String path = addr.getString(2);
        return connect(path);
    }

    @Override
    public int write(byte[] data) {
        try {
            handler.handle(data);
            return data.length;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private int connect(String path) {
        if ("/dev/socket/logdw".equals(path)) {
            handler = new UdpHandler() {
                private static final int LOG_ID_MAIN = 0;
                private static final int LOG_ID_RADIO = 1;
                private static final int LOG_ID_EVENTS = 2;
                private static final int LOG_ID_SYSTEM = 3;
                private static final int LOG_ID_CRASH = 4;
                private static final int LOG_ID_KERNEL = 5;
                private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                @Override
                public void handle(byte[] request) {
                    try {
                        byteArrayOutputStream.write(request);

                        if (byteArrayOutputStream.size() <= 11) {
                            return;
                        }

                        int tagIndex = -1;
                        int bodyIndex = -1;
                        byte[] body = byteArrayOutputStream.toByteArray();
                        ByteBuffer buffer = ByteBuffer.wrap(body);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        int id = buffer.get() & 0xff;
                        int tid = buffer.getShort() & 0xffff;
                        int tv_sec = buffer.getInt();
                        int tv_nsec = buffer.getInt();
                        //log.debug("handle id=" + id + ", tid=" + tid + ", tv_sec=" + tv_sec + ", tv_nsec=" + tv_nsec);

                        String type;
                        switch (id) {
                            case LOG_ID_MAIN:
                                type = "main";
                                break;
                            case LOG_ID_RADIO:
                                type = "radio";
                                break;
                            case LOG_ID_EVENTS:
                                type = "events";
                                break;
                            case LOG_ID_SYSTEM:
                                type = "system";
                                break;
                            case LOG_ID_CRASH:
                                type = "crash";
                                break;
                            case LOG_ID_KERNEL:
                                type = "kernel";
                                break;
                            default:
                                type = Integer.toString(id);
                                break;
                        }

                        for (int i = 12; i < body.length; i++) {
                            if (body[i] != 0) {
                                continue;
                            }

                            if (tagIndex == -1) {
                                tagIndex = i;
                                continue;
                            }

                            bodyIndex = i;
                            break;
                        }

                        if (tagIndex != -1 && bodyIndex != -1) {
                            byteArrayOutputStream.reset();

                            int level = body[11] & 0xff;
                            String tag = new String(body, 12, tagIndex - 12);
                            String text = new String(body, tagIndex + 1, bodyIndex - tagIndex - 1);
                            LogCatLevel value = LogCatLevel.valueOf(level);
                            //LinuxFileSystem fileSystem = (LinuxFileSystem) emulator.getFileSystem();
                            //LogCatHandler handler = fileSystem.getLogCatHandler();
                            //if (handler != null) {
                            //    handler.handleLog(type, value, tag, text);
                            //} else {
                                System.err.printf("[%s]%s/%s: %s%n", type, value, tag, text);
                            //}
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            return 0;
        }
        //emulator.getMemory().setErrno(UnixEmulator.EPERM);
        return -1;
    }
}
