package com.dta.lesson53.source;

import com.github.unidbg.*;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.Dobby;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class MainActivity {

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson53/source/libnative-lib.so"), false);
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();
    }

    public String stringFromJNI() {
        DvmObject<?> obj = vm.resolveClass("com/dta/inlinehookcheck/MainActivity").newObject(null);
        //public native String stringFromJNI();
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        Object value = dvmObject.getValue();
        System.out.println(value);
        return value.toString();
    }
}
