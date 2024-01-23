package com.dta.lesson51;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class AntiMethodID extends AbstractJni {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        AntiMethodID antiFinclass = new AntiMethodID();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        antiFinclass.stringFromJNI();
    }

    private final AndroidEmulator emulator;
    private final VM vm;

    private AntiMethodID() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson51/methodID.so"), false);
        dm.callJNI_OnLoad(emulator);
    }

    private void stringFromJNI() {
        DvmObject<?> obj = vm.resolveClass("com/dta/anti_methodid/MainActivity").newObject(null);
        //public native String stringFromJNI();
        DvmObject<?> zhangsan = vm.resolveClass("com/dta/anti_methodid/ZhangSan", vm.resolveClass("com/dta/anti_methodid/Person")).newObject(null);
        DvmObject<?> dvmObject = obj.callJniMethodObject(emulator, "stringFromJNI(Lcom/dta/anti_methodid/ZhangSan;)Ljava/lang/String;", zhangsan);
        Object value = dvmObject.getValue();
        System.out.println(value);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("com/dta/anti_methodid/Person->getName()Ljava/lang/String;")){
            System.out.println(dvmObject);
            return new StringObject(vm, "Zhangsan");
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }
}
