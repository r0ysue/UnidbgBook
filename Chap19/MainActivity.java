package com.dta.lesson29;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;


public class MainActivity extends AbstractJni {
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
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson29/libJNIEncrypt.so"), false);
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
        //mainActivity.debuger();
        mainActivity.patch_signature();
        //mainActivity.doRawData();
        mainActivity.decode();

    }

    private void decode() {
        DvmClass Encryto = vm.resolveClass("com/tencent/testvuln/c/Encryto");
//        DvmObject<?> obj = vm.resolveClass("android/content/Context").newObject(null);
        String input = "9YuQ2dk8CSaCe7DTAmaqAA==";
        DvmObject<?> dvmObject = Encryto.callStaticJniMethodObject(emulator, "decode(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",0,input);
        System.out.println("decode result ==> " + dvmObject.getValue());
    }

    private void patch_signature() {
        //3948
        byte[] code = new byte[]{0x01,0x20,0x00, (byte) 0xBF};

        emulator.getBackend().mem_write(module.base+0x3948,code);
        //3888
        emulator.getBackend().mem_write(module.base+0x3888,code);
    }

    private void debuger() {
        emulator.attach().addBreakPoint(module,0x3948);
    }

    private void doRawData() {
        DvmClass Encryto = vm.resolveClass("com/tencent/testvuln/c/Encryto");
//        DvmObject<?> obj = vm.resolveClass("android/content/Context").newObject(null);
        String input = "123";
        DvmObject<?> dvmObject = Encryto.callStaticJniMethodObject(emulator, "doRawData(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",0,input);
        System.out.println("doRawdata result ==> " + dvmObject.getValue());
    }
}
