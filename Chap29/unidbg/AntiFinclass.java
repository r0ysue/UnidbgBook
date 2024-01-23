package com.dta.lesson51;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class AntiFinclass {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        AntiFinclass antiFinclass = new AntiFinclass();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        antiFinclass.stringFromJNI();
    }

    private final AndroidEmulator emulator;
    private final VM vm;

    private AntiFinclass() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson51/findclass.so"), false);
        dm.callJNI_OnLoad(emulator);
    }

    private void stringFromJNI() {
        vm.addNotFoundClass("com/dta/anti_findclass/MainActivity2");
        DvmObject<?> obj = vm.resolveClass("com/dta/anti_findclass/MainActivity").newObject(null);
        //public native String stringFromJNI();
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        Object value = dvmObject.getValue();
        System.out.println(value);
    }
}
