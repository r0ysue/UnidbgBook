package com.dta.lesson45;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class MainActivity {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        mainActivity.getSalt();
    }

    private final AndroidEmulator emulator;
    private final VM vm;

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson45/libnative-lib.so"), false);
        dm.callJNI_OnLoad(emulator);
    }

    private void getSalt() {
        DvmObject<?> obj = vm.resolveClass("com/dta/lesson45/MainActivity").newObject(null);
        //public native String getSalt();
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "getSalt()Ljava/lang/String;");
        Object value = dvmObject.getValue();
        System.out.println(value);
    }
}
