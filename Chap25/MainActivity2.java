package com.dta.lesson35;

import com.dta.lesson34.Base64;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


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

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson35/DogPlus.apk"));
        vm.setVerbose(true);
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary("dogplus", false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);

        obj = vm.resolveClass("com/example/dogplus/MainActivity").newObject(null);
    }



    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity = new MainActivity2();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.detectAccessibilityManager();
    }
    //public native void detectAccessibilityManager();
    private void detectAccessibilityManager(){
        obj.callJniMethod(emulator,"detectAccessibilityManager()V");
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")){
            return vm.resolveClass("android/app/Application").newObject(null);
        }
        if (signature.equals("android/view/accessibility/AccessibilityManager->getInstalledAccessibilityServiceList()Ljava/util/List;")){
            List<DvmObject<?>> list = new ArrayList<>();
            //AccessibilityServiceInfo
            DvmClass dvmClass = vm.resolveClass("android/accessibilityservice/AccessibilityServiceInfo");
            AccessibilityServiceInfo info1 = new AccessibilityServiceInfo("TalkBackService","com.google.android.marvin.talkback","TalkBack");
            AccessibilityServiceInfo info2= new AccessibilityServiceInfo("SelectToSpeakService","com.google.android.marvin.talkback","随选朗读");
            list.add(dvmClass.newObject(info1));
            list.add(dvmClass.newObject(info2));
            return new ArrayListObject(vm,list);
        }
        if (signature.equals("android/accessibilityservice/AccessibilityServiceInfo->getResolveInfo()Landroid/content/pm/ResolveInfo;")){
            //dvmObject
            return vm.resolveClass("android/content/pm/ResolveInfo").newObject(dvmObject.getValue());
        }
        if (signature.equals("android/content/pm/ServiceInfo->loadLabel(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;")){
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return vm.resolveClass("java/lang/CharSequence").newObject(info.lable);
        }
        if (signature.equals("java/lang/CharSequence->toString()Ljava/lang/String;")){
            return new StringObject(vm,dvmObject.getValue().toString());
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        if (signature.equals("android/content/pm/ResolveInfo->serviceInfo:Landroid/content/pm/ServiceInfo;")){

            return vm.resolveClass("android/content/pm/ServiceInfo").newObject(dvmObject.getValue());
        }
        if (signature.equals("android/content/pm/ServiceInfo->name:Ljava/lang/String;")){
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return new StringObject(vm, info.name);
        }
        if (signature.equals("android/content/pm/ServiceInfo->packageName:Ljava/lang/String;")){
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return new StringObject(vm, info.packageName);
        }
        if (signature.equals("android/accessibilityservice/AccessibilityServiceInfo->packageNames:[Ljava/lang/String;")){
            return new ArrayObject();
        }
        return super.getObjectField(vm, dvmObject, signature);
    }
}
