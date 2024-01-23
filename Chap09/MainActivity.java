package com.dta.lesson2;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.sun.jna.Pointer;
import net.dongliu.apk.parser.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

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

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson2/app-debug.apk"));

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/libtest-lib.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    public void callAes(){
        DvmObject obj = ProxyDvmObject.createObject(vm,this);
        obj.callJniMethod(emulator, "aes(II)V");
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        mainActivity.keyFinder();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.callAes();
    }

    private void keyFinder() {
        List<String> funclist = readFuncFromIDA("unidbg-android/src/test/java/com/dta/lesson2/libtest-lib_functionlist_1636779320.txt");
        AesKeyFinder aesKeyFinder = new AesKeyFinder(emulator);
        aesKeyFinder.searchEveryFunction(module.base, funclist);
    }


}
