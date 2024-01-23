package com.dta.r0dbg.android.emulate;


import capstone.Capstone;
import com.dta.r0dbg.android.module.ElfModule;
import com.dta.r0dbg.linux.SystemCallHandler;
import com.dta.r0dbg.linux.file.FileSystem;
import com.dta.r0dbg.linux.file.IFileSystem;
import com.dta.r0dbg.memory.Memory;
import com.dta.r0dbg.memory.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;
import unicorn.CodeHook;
import unicorn.Unicorn;

import com.dta.r0dbg.bakend.BackendType;
import com.dta.r0dbg.bakend.IBackend;
import com.dta.r0dbg.bakend.unicorn.UnicornBackend;
import com.dta.r0dbg.util.FileHelper;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class AndroidEmulate implements IEmulate {

    private static final Logger logger = LoggerFactory.getLogger(AndroidEmulate.class);

    boolean running = false;

    private String processName = "xxdbg";

    private IFileSystem fileSystem = null;

    private final int pid;

    private AndroidEmulate() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        this.pid = Integer.parseInt(pid);
    }

    /**
     * 32位/64位标识
     */
    private boolean is32Bit = true;

    /**
     * 初始化操作
     */
    @Override
    public void init() {
        fileSystem = new FileSystem(this);

        //开启VFP
        backend.enableVFP();
        //初始化内存(栈 \ LR)
        getMemory().init();
        // 系统调用处理
        backend.hook_add_new(new SystemCallHandler(), this);

        initializeTLS(new String[] {
                "ANDROID_DATA=/data",
                "ANDROID_ROOT=/system"
        });

    }

    /**
     * http://androidxref.com/6.0.0_r5/xref/bionic/libc/bionic/libc_init_common.cpp
     */
    Pointer errno = null;
    private Pointer initializeTLS(String[] envs) {
        final Pointer thread = memory.allocateStack(0x400);

        final Pointer __stack_chk_guard = memory.allocateStack(getPointSize());

        final Pointer programName = memory.writeStackString(getProcessName());

        final Pointer programNamePointer = memory.allocateStack(getPointSize());
        assert programNamePointer != null;
        programNamePointer.setPointer(0, programName);

        final Pointer auxv = memory.allocateStack(0x100);
        assert auxv != null;
        if (is32Bit()) {
            auxv.setInt(0, 25);
        } else {
            auxv.setLong(0, 25);
        }
        auxv.setPointer(getPointSize(), __stack_chk_guard);

        List<String> envList = new ArrayList<>();
        for (String env : envs) {
            int index = env.indexOf('=');
            if (index != -1) {
                envList.add(env);
            }
        }
        final Pointer environ = memory.allocateStack(getPointSize() * (envList.size() + 1));
        assert environ != null;
        Pointer pointer = environ;
        for (String env : envList) {
            Pointer envPointer = memory.writeStackString(env);
            pointer.setPointer(0, envPointer);
            pointer = pointer.share(getPointSize());
        }
        pointer.setPointer(0, null);

        final Pointer argv = memory.allocateStack(0x100);
        assert argv != null;
        argv.setPointer(getPointSize(), programNamePointer);
        argv.setPointer(2L * getPointSize(), environ);
        argv.setPointer(3L * getPointSize(), auxv);

        final Pointer tls = memory.allocateStack(0x80 * 4); // tls size
        assert tls != null;
        tls.setPointer(getPointSize(), thread);
        this.errno = tls.share(getPointSize() * 2L);
        tls.setPointer(getPointSize() * 3L, argv);

        if (is32Bit()) {
            backend.reg_write(ArmConst.UC_ARM_REG_C13_C0_3, tls.peer);
        } else {
            backend.reg_write(Arm64Const.UC_ARM64_REG_TPIDR_EL0, tls.peer);
        }

        long sp = memory.getStackPoint();
        sp &= (~(is64Bit() ? 15 : 7));
        memory.setStackPoint(sp);

//        if (log.isDebugEnabled()) {
        debug("initializeTLS tls=" + tls + ", argv=" + argv + ", auxv=" + auxv + ", thread=" + thread + ", environ=" + environ + ", sp=0x" + Long.toHexString(memory.getStackPoint()));
//        }
        return argv.share(2L * getPointSize(), 0);
    }

    public boolean is32Bit() {
        return is32Bit;
    }

    public boolean is64Bit() {
        return !is32Bit;
    }

    /**
     * 后端实例
     */
    IBackend backend = null;

    public IBackend getBackend() {
        if (backend == null) {
            throw new AndroidEmulateException("the backend is null");
        }
        return backend;
    }

    /**
     * 内存实例
     */
    Memory memory = null;

    public Memory getMemory() {
        if (memory == null) {
            throw new AndroidEmulateException("the memory is null");
        }
        return memory;
    }

    public File getSystemLibrary(String name) {
        //name = name.replaceAll("\\+","p");
        File libFile = FileHelper.getResourceFile(AndroidEmulate.class, "android/ld/" + name);
        return libFile;
    }

    /**
     * 执行函数
     *
     * @param begin
     * @param until
     * @return
     */
    @Override
    public Number eFunc(long begin, long until) {
        backend.reg_write(ArmConst.UC_ARM_REG_LR, LR);
        return emulate(begin, LR, 0);
    }

    @Override
    public void traceCode(long begin, long end) {
        final Capstone cs_thumb = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_THUMB);
        final Capstone cs_arm = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_ARM);
        backend.hook_add_new(new CodeHook() {
            public void hook(Unicorn u, long address, int size, Object user) {
                byte[] code = u.mem_read(address, size);
                Capstone.CsInsn[] disasm;
                Long cpsr = (Long) u.reg_read(ArmConst.UC_ARM_REG_CPSR);
                if ((cpsr >>> 5 & 0x1) == 1) {
                    disasm = cs_thumb.disasm(code, address);
                } else {
                    disasm = cs_arm.disasm(code, address);
                }
                ElfModule elfModule = getMemory().getElfModuleByAddress(address);
                System.out.println(String.format(">>> Tracing ins at [%20s] [0x%x]: %s %s", elfModule.getName(), disasm[0].address - elfModule.getBase(), disasm[0].mnemonic, disasm[0].opStr));
            }
        }, begin, end, null);
    }

    @Override
    public int getPointSize() {
        return is32Bit ? 4 : 8;
    }

    @Override
    public Number eInit(long begin) {
        return eFunc(begin, LR);
    }

    @Override
    public String getProcessName() {
        return processName;
    }

    @Override
    public Pointer getErrnoPointer() {
        return errno;
    }

    @Override
    public IFileSystem getFileSystem() {
        return fileSystem;
    }


    protected final Number emulate(long begin, long until, long timeout) {
        if (running) {
            backend.emu_stop();
            throw new IllegalStateException("running");
        }

        try {

            if (logger.isDebugEnabled()) {
                //logger.debug("emulate " + pointer + " started sp=" + getStackPointer());
            }

            running = true;
            if (logger.isDebugEnabled()) {

            }
            backend.emu_start(begin, until, timeout, 0);

            if (is64Bit()) {
                return backend.reg_read(Arm64Const.UC_ARM64_REG_X0);
            } else {
                Number r0 = backend.reg_read(ArmConst.UC_ARM_REG_R0);
                Number r1 = backend.reg_read(ArmConst.UC_ARM_REG_R1);
                return (r0.intValue() & 0xffffffffL) | ((r1.intValue() & 0xffffffffL) << 32);
            }
        } catch (RuntimeException e) {
            debug("emulate runtime exception.message:%s", e.getMessage());
            throw e;
        } finally {
            running = false;
        }
    }

//    Debugger debugger;

    // 默认使用Unicorn
    BackendType backendType;

    /**
     * 建造类
     */
    public static class Builder {
        private boolean is32Bit = true;
        private BackendType backendType = BackendType.Unicorn;

        public Builder() {
            //Logger.getLogger(AndroidEmulate.class).info("123");
            //PropertyConfigurator.configure(AndroidEmulate.class.getClassLoader().getResource("log4j.properties"));
            //BasicConfigurator.configure();
        }

        public Builder for32Bit() {
            is32Bit = true;
            return this;
        }

        public Builder for64Bit() {
            is32Bit = false;
            return this;
        }

        public Builder setBackendType(BackendType backendType) {
            this.backendType = backendType;
            return this;
        }

        public AndroidEmulate build() {
            AndroidEmulate emulate = new AndroidEmulate();
            emulate.is32Bit = this.is32Bit;
            emulate.backendType = this.backendType;

            switch (emulate.backendType) {
                case Unicorn:
                    emulate.backend = new UnicornBackend(is32Bit);
                    break;

                default:
                    throw new AndroidEmulateException("Unknown the backend type!");
            }
            emulate.memory = new Memory(emulate);
            emulate.init();
            return emulate;
        }
    }

    private void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }

    public static String byte2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String s = Integer.toHexString(b & 0xff);
            if (s.length() < 2)
                sb.append("0");
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public int getPageAlign() {
        return 0x1000;
    }

    @Override
    public int getPid() {
        return 0;
    }
}
