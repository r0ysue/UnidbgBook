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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/libnative-lib.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    public void callMd5(){
        DvmObject obj = ProxyDvmObject.createObject(vm,this);
        String data = "dta";
        DvmObject dvmObject = obj.callJniMethodObject(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", data);
        String result = (String) dvmObject.getValue();
        System.out.println("[symble] Call the so md5 function result is ==> "+ result);
    }

    private void call_address() {
        Pointer jniEnv = vm.getJNIEnv();
        DvmObject obj = ProxyDvmObject.createObject(vm,this);
        StringObject data = new StringObject(vm,"dta");

        List<Object> args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addLocalObject(obj));
        args.add(vm.addLocalObject(data));

        Number[] numbers = module.callFunction(emulator, 0x8E81, args.toArray());
        DvmObject<?> object = vm.getObject(numbers[0].intValue());
        String value = (String) object.getValue();
        System.out.println("[addr] Call the so md5 function result is ==> "+ value);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.callMd5();
        mainActivity.call_address();
    }

}
