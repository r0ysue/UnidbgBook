package com.dta.r0dbg.linux;

import unicorn.ArmConst;
import unicorn.InterruptHook;
import unicorn.Unicorn;

public class SystemCallHandler implements InterruptHook {
    @Override
    public void hook(Unicorn u, int intno, Object user) {
        Long R7 = (Long) u.reg_read(ArmConst.UC_ARM_REG_R7);
        System.out.println("syscall NR:"+R7.intValue());
    }
}
