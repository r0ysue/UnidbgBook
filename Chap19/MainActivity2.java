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


public class MainActivity2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity2(){
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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson29/libnative.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    static {
        Logger.getLogger(AndroidElfLoader.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity = new MainActivity2();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.ncheck();

    }

    private void ncheck() {
        //com.a.easyjni.MainActivity
        DvmObject<?> dvmObject = vm.resolveClass("com/a/easyjni/MainActivity").newObject(null);
        String input = "QAoOQMPFks1BsB7cbM3TQsXg30i9g3==";
        boolean b = dvmObject.callJniMethodBoolean(emulator, "ncheck(Ljava/lang/String;)Z",input);
        System.out.println("result ==> " + b);
    }

}
