package com.dta.lesson36;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;


public class MethodID extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;
    private final DvmObject<?> obj;
    private DvmClass MainActivity;

    public MethodID(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson35/MethodID.apk"));
        vm.setVerbose(true);
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary("getpackagename", false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);

        DvmClass Context = vm.resolveClass("android/content/Context");
        DvmClass ContextWrapper = vm.resolveClass("android/content/ContextWrapper",Context);

        MainActivity = vm.resolveClass("com/example/getpackagename/MainActivity",ContextWrapper);
        obj = MainActivity.newObject(null);
    }



    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MethodID mainActivity = new MethodID();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.getAppName();
    }

    ///public native void getAppName();
    private void getAppName(){
        obj.callJniMethod(emulator,"getAppName()V");
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")){
            return vm.resolveClass("android/app/Application", MainActivity).newObject(null);
        }
        if (signature.equals("com/example/getpackagename/MainActivity->getPackageName()Ljava/lang/String;")){
            return new StringObject(vm, vm.getPackageName());
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

//    @Override
//    public boolean acceptMethod(DvmClass dvmClass, String signature, boolean isStatic) {
//        if (signature.equals("com/example/getpackagename/MainActivity->getPackageName()Ljava/lang/String;")){
//            return false;
//        }
//        return super.acceptMethod(dvmClass, signature, isStatic);
//    }
}
