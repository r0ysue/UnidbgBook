package com.dta.r0dbg.android.emulate;


import com.dta.r0dbg.memory.Memory;
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

public final class AndroidEmulate implements IEmulate {

    private static final Logger logger = LoggerFactory.getLogger(AndroidEmulate.class);

    boolean running = false;

    private AndroidEmulate() {
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
        //开启VFP
        //backend.enableVFP();
        //初始化内存(栈 \ LR)
        //getMemory().init();
        // 系统调用处理
        //backend.hook_add_new(new SystemCallHandler(), this);

        //initializeTLS();

    }

    /**
     * http://androidxref.com/6.0.0_r5/xref/bionic/libc/bionic/libc_init_common.cpp
     */
    private void initializeTLS() {
        //backend.mem_map(0x1000,0x1000,Unicorn.UC_PROT_ALL);
        //backend.reg_write(ArmConst.UC_ARM_REG_C13_C0_3, 0x1000);
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
//        final Capstone cs_thumb = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_THUMB);
//        final Capstone cs_arm = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_ARM);
//        backend.hook_add_new(new CodeHook() {
//            public void hook(Unicorn u, long address, int size, Object user) {
//                if (Long.toHexString(address).endsWith("27e")) {
//                    System.out.println(123);
//                }
//                byte[] code = u.mem_read(address, size);
//                Capstone.CsInsn[] disasm;
//                Long cpsr = (Long) u.reg_read(ArmConst.UC_ARM_REG_CPSR);
//                if ((cpsr >>> 5 & 0x1) == 1) {
//                    disasm = cs_thumb.disasm(code, address);
//                } else {
//                    disasm = cs_arm.disasm(code, address);
//                }
//                ElfModule elfModule = getMemory().getElfModuleByAddress(address);
//                System.out.println(String.format(">>> Tracing ins at [%20s] [0x%x]: %s %s", elfModule.getName(), disasm[0].address - elfModule.getBase(), disasm[0].mnemonic, disasm[0].opStr));
//            }
//        }, begin, end, null);
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
}
