package com.dta.r0dbg.bakend.unicorn;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unicorn.*;

import com.dta.r0dbg.bakend.BackendException;
import com.dta.r0dbg.bakend.IBackend;
import java.util.HashMap;

public class UnicornBackend implements IBackend {
    private static final Logger logger = LoggerFactory.getLogger(UnicornBackend.class);
    private Unicorn unicorn;
    private boolean is64Bit = false;


    public UnicornBackend(boolean is32Bit) {
        is64Bit = !is32Bit;
        unicorn = new Unicorn(is32Bit ? Unicorn.UC_ARCH_ARM : Unicorn.UC_ARCH_ARM64, Unicorn.UC_MODE_ARM);
    }

    @Override
    public void enableVFP() {

        if (is64Bit) {
            long value = reg_read(Arm64Const.UC_ARM64_REG_CPACR_EL1).longValue();
            value |= 0x300000; // set the FPEN bits
            reg_write(Arm64Const.UC_ARM64_REG_CPACR_EL1, value);
        } else {
            int value = reg_read(ArmConst.UC_ARM_REG_C1_C0_2).intValue();
            value |= (0xf << 20);
            reg_write(ArmConst.UC_ARM_REG_C1_C0_2, value);
            reg_write(ArmConst.UC_ARM_REG_FPEXC, 0x40000000);
        }
    }

    public Number reg_read(int regId) throws BackendException {
        return (Long) unicorn.reg_read(regId);
    }

    public byte[] reg_read_vector(int regId) throws BackendException {
        return new byte[0];
    }

    public void reg_write_vector(int regId, byte[] vector) throws BackendException {

    }

    public void reg_write(int regId, Number value) throws BackendException {
        unicorn.reg_write(regId, value);
    }

    public byte[] mem_read(long address, long size) throws BackendException {
        return unicorn.mem_read(address, size);
    }

    public void mem_write(long address, byte[] bytes) throws BackendException {
        try {
            unicorn.mem_write(address, bytes);
        }catch (Exception e){
            logger.debug(String.format("unicorn backend mem_write exception address: 0x%x",address));
            throw e;
        }

    }

    public void mem_map(long address, long size, int perms) throws BackendException {
        logger.debug(String.format("unicorn backend mem_map address: 0x%x, size: 0x%x, perms: %d",address,size,perms));
        unicorn.mem_map(address, size, perms);
    }

    public void mem_protect(long address, long size, int perms) throws BackendException {
        logger.debug(String.format("unicorn backend mem_protect address: 0x%x, size: 0x%x, perms: %d",address,size,perms));
        unicorn.mem_protect(address, size, perms);
    }

    public void mem_unmap(long address, long size) throws BackendException {
        unicorn.mem_unmap(address, size);
    }

    public boolean removeBreakPoint(long address) {
        return false;
    }

    public void setSingleStep(int singleStep) {
        throw new BackendException("Unsupported");
    }

    public void setFastDebug(boolean fastDebug) {
        throw new BackendException("Unsupported");
    }

    public void hook_add_new(CodeHook callback, long begin, long end, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, begin, end, user_data);
    }

    public void debugger_add(DebugHook callback, long begin, long end, Object user_data) throws BackendException {
        unicorn.debugger_add(callback, begin, end, user_data);
    }

    public void hook_add_new(ReadHook callback, long begin, long end, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, begin, end, user_data);
    }

    public void hook_add_new(WriteHook callback, long begin, long end, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, begin, end, user_data);
    }

    public void hook_add_new(EventMemHook callback, int type, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, type, user_data);
    }

    public void hook_add_new(InterruptHook callback, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, user_data);
    }

    public void hook_add_new(BlockHook callback, long begin, long end, Object user_data) throws BackendException {
        unicorn.hook_add_new(callback, begin, end, user_data);
    }

    public void emu_start(long begin, long until, long timeout, long count) throws BackendException {
        unicorn.emu_start(begin, until, timeout, count);
    }

    public void emu_stop() throws BackendException {
        unicorn.emu_stop();
    }

    public void destroy() throws BackendException {
        unicorn.closeAll();
    }

    public void context_restore(long context) {

    }

    public void context_save(long context) {

    }

    public long context_alloc() {
        return 0;
    }

    public int getPageSize() {
        return 0;
    }
}
