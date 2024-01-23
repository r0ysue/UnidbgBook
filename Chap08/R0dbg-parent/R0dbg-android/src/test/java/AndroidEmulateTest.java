import com.bxlong.elf.ElfSymbol;
import com.dta.r0dbg.android.emulate.AndroidEmulate;
import com.dta.r0dbg.android.module.ElfModule;
import com.dta.r0dbg.bakend.BackendType;
import com.dta.r0dbg.memory.Memory;
import com.dta.r0dbg.util.FileHelper;
import unicorn.ArmConst;

import java.io.File;

public class AndroidEmulateTest {

    public static void main(String[] args) {
        AndroidEmulate emulate = new  AndroidEmulate.Builder()
                .for32Bit()
                .setBackendType(BackendType.Unicorn)
                .build();
        Memory memory = emulate.getMemory();
        File file = FileHelper.getResourceFile(AndroidEmulateTest.class,"example/libtest-lib.so");
        emulate.traceCode(0,-1);
        ElfModule elfModule = memory.loadLibrary(file, true);
        ElfSymbol add = elfModule.getElfFile().getDynamicSegment().getDynamicStructure().getSymbolTable().getValue().getELFSymbolByName("Java_com_dta_lesson5_MainActivity_add");

        emulate.getBackend().reg_write(ArmConst.UC_ARM_REG_R2,0);
        emulate.getBackend().reg_write(ArmConst.UC_ARM_REG_R3,10);
        Number number = emulate.eFunc(add.value + elfModule.getBase(), 0);
        System.out.println("return value is ==> " + number.intValue());
    }

}
