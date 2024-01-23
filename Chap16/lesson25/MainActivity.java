package com.dta.lesson25;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.file.FileIO;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.file.linux.StatStructure;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.linux.struct.StatFS;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneMode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import unicorn.Unicorn;

import java.io.File;
import java.io.IOException;


public class MainActivity implements IOResolver<AndroidFileIO> {
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

        emulator.getSyscallHandler().addIOResolver(this);

        vm = emulator.createDalvikVM();
        vm.setVerbose(true);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25/libmyjni.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);

        emulator.attach().addBreakPoint(module,0x1256);
    }

    static {
        Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.saveSN();
    }

    private void saveSN() {
        DvmObject<?> dvmObject = vm.resolveClass("com/gdufs/xman/MyApp").newObject(null);
        String arg = "EoPAoY62@ElRD";
        dvmObject.callJniMethod(emulator, "saveSN(Ljava/lang/String;)V",arg);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        if (pathname.equals("/sdcard/reg.dat")){
            File reg = new File("unidbg-android/src/test/java/com/dta/lesson25/reg.dat");
            return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags,reg,pathname));
        }
        return null;
    }
}
