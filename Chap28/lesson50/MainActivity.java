package com.dta.lesson50;

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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class MainActivity {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        //mainActivity.hook();
        mainActivity.inlinehook();
        mainActivity.stringFromJNI();
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

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson50/libnative-lib.so"), false);
        dm.callJNI_OnLoad(emulator);
    }

    private void stringFromJNI() {
        DvmObject<?> obj = vm.resolveClass("com/dta/gotcheck/MainActivity").newObject(null);
        //public native String stringFromJNI();
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        Object value = dvmObject.getValue();
        System.out.println(value);
    }
}
