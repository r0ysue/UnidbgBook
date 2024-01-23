package com.dta.lesson33;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.memory.Memory;

import java.io.File;


public class MainActivity2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;
    private final DvmObject<?> obj;
    private static int count = 0;

    public MainActivity2(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson33/DogLite.apk"));
        vm.setVerbose(true);
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson33/libdoglite.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);

        obj = vm.resolveClass("com/example/doglite/MainActivity").newObject(null);
    }



    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity = new MainActivity2();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        //mainActivity.detectFile();
        mainActivity.detectFileNew();

    }

    private void detectFileNew() {
        obj.callJniMethod(emulator, "detectFileNew()V");
    }

    private void detectFile() {
        obj.callJniMethod(emulator, "detectFile()V");
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if (signature.equals("java/io/File-><init>(Ljava/lang/String;)V")){
            String path = (String) vaList.getObjectArg(0).getValue();
            System.err.println("path:"+path);
            return vm.resolveClass("java/io/File").newObject(path);
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/io/File->exists()Z")){

            String key =  dvmObject.getValue().toString();
            String path = emulator.get(key);
            //String path = (String) dvmObject.getValue();
            if (path.equals("/sys/class/power_supply/battery/voltage_now")){
                return true;
            }else if (path.equals("/data/local/tmp/Nox")){
                return false;
            }else if (path.equals("/data/local/tmp/nox")){
                return false;
            }

            // count TAG
//            String tag = (String) dvmObject.getValue().toString();
//            if (tag.equals("1")){
//                return true;
//            }else if(tag.equals("2")){
//                return false;
//            }
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        if (signature.equals("java/io/File->allocObject")){
            long m = System.currentTimeMillis();
            return vm.resolveClass("java/io/File").newObject(m);
        }
        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/io/File-><init>(Ljava/lang/String;)V")){
            String path = (String) vaList.getObjectArg(0).getValue();
            System.err.println("File<init>() path:"+path);
            String key =  dvmObject.getValue().toString();
            emulator.set(key, path);
            return;
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    private int getCount(){
        return ++count;
    }
}
