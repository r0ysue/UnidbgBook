import com.dta.r0dbg.android.emulate.AndroidEmulate;
import com.dta.r0dbg.android.module.ElfModule;
import com.dta.r0dbg.bakend.BackendType;
import com.dta.r0dbg.memory.Memory;
import com.dta.r0dbg.util.FileHelper;

import java.io.File;

public class AndroidEmulateTest {

    public static void main(String[] args) {
        AndroidEmulate emulate = new  AndroidEmulate.Builder()
                .for32Bit()
                .setBackendType(BackendType.Unicorn)
                .build();
        Memory memory = emulate.getMemory();
        File file = FileHelper.getResourceFile(AndroidEmulateTest.class,"example/libtest-lib.so");
        ElfModule elfModule = memory.loadLibrary(file, true);
    }

}
