package com.dta.lesson52;

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

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        //mainActivity.trace();
        //mainActivity.hook();
        //mainActivity.inlinehook();
        mainActivity.stringFromJNI();
    }

    private void trace() {
        Symbol z9say_hellov = emulator.getMemory().findModule("libnative-lib.so").findSymbolByName("_Z9say_hellov");
        long start = z9say_hellov.getAddress();

        emulator.traceCode(start, start + 60);
    }

    private void inlinehook() {
        Dobby instance = Dobby.getInstance(emulator);
        Symbol z9say_hellov = emulator.getMemory().findModule("libnative-lib.so").findSymbolByName("_Z9say_hellov");

        instance.replace(z9say_hellov, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("inline hook!!!!!");
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                super.postCall(emulator, context);
            }
        });
    }

    private void hook() {
        IxHook ixHook = XHookImpl.getInstance(emulator);
        ixHook.register(".*native-lib\\.so", "_Z9say_hellov", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                super.postCall(emulator, context);
            }
        });

        ixHook.refresh();

    }

    static{
        //Logger.getLogger(AbstractEmulator.class).setLevel(Level.DEBUG);
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
//        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson52/libnative-lib.so"), false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson52/libnative-lib-dl-iterater-phdr.so"), false);
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();
    }

    private void stringFromJNI() {

        DvmObject<?> obj = vm.resolveClass("com/dta/inlinehookcheck/MainActivity").newObject(null);
        //public native String stringFromJNI();
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        Object value = dvmObject.getValue();
        System.out.println(value);
    }
}
