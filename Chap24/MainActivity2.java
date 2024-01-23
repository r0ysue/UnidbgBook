package com.dta.lesson34;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Arrays;



public class MainActivity2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;
    private final DvmObject<?> obj;

    public MainActivity2(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson34/DogLite.apk"));
        vm.setVerbose(true);
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson34/libdoglite.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);

        obj = vm.resolveClass("com/example/doglite/MainActivity").newObject(null);
    }



    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity = new MainActivity2();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
//        mainActivity.SysInfo();
//        mainActivity.getAppFilesDir();
        mainActivity.base64result();
    }
    //public native void base64result(String str);
    private void base64result() {
        String input = "12345";
        obj.callJniMethod(emulator,"base64result(Ljava/lang/String;)V",input);
    }


    private void getAppFilesDir() {
        obj.callJniMethod(emulator,"getAppFilesDir()V");
    }

    private void SysInfo() {
        obj.callJniMethod(emulator,"SysInfo()V");
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")){
            return vm.resolveClass("android/app/Application").newObject(null);
        }
        if (signature.equals("java/io/File->getAbsolutePath()Ljava/lang/String;")){
            String tag = dvmObject.getValue().toString();
            if (tag.equals("android/os/Environment->getExternalStorageDirectory()Ljava/io/File;")){
                return new StringObject(vm,"/sdcard/");
            }else if (tag.equals("android/os/Environment->getStorageDirectory()Ljava/io/File;")){
                return new StringObject(vm, "/");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if (signature.equals("android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;")){
            String arg1 = vaList.getObjectArg(1).getValue().toString();
            System.err.println("getString() arg1:"+arg1);
            return new StringObject(vm, "123456789");
        }
        if (signature.equals("android/os/Environment->getExternalStorageDirectory()Ljava/io/File;")){
            return vm.resolveClass("java/io/File").newObject(signature);
        }
        if (signature.equals("android/os/Environment->getStorageDirectory()Ljava/io/File;")){
            return vm.resolveClass("java/io/File").newObject(signature);
        }
        if (signature.equals("android/util/Base64->encodeToString([BI)Ljava/lang/String;")){
            byte[] input = (byte[]) vaList.getObjectArg(0).getValue();
            int flag = vaList.getIntArg(1);
            String s = Base64.encodeToString(input, flag);
            return new StringObject(vm, s);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }


}
