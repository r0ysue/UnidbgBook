package com.dta.lesson27;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;


public class MainActivity {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM();
        vm.setVerbose(true);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson27/libcyberpeace.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    static {
        Logger.getLogger(AndroidElfLoader.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.debugger();
        //mainActivity.check();
        mainActivity.callAddress();

    }

    private void debugger() {
        emulator.attach().addBreakPoint(module,0x10B8);
    }

    private void check() {
        DvmClass obj = vm.resolveClass("com/testjava/jack/pingan2/cyberpeace");
        //public static native int CheckString(String str);
        String input = "123456654321abcdeffedcba4321abcd";
        int i = obj.callStaticJniMethodInt(emulator, "CheckString(Ljava/lang/String;)I", input);
        System.out.println("result  ==> "+ i);
    }

    private void callAddress(){
        emulator.traceCode();
        UnidbgPointer buffer = memory.malloc(32, false).getPointer();
        buffer.setString(0,"f72c5a36569418a20907b55be5bf95ad");

        Backend backend = emulator.getBackend();
        backend.reg_write(ArmConst.UC_ARM_REG_R4,buffer.peer);
        module.callFunction(emulator,0x108B);
    }
}
