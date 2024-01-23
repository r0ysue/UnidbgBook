package com.dta.lesson2;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MainJni extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainJni(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson2/app-debug.apk"));
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/libjni.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    public void callMd5(){
        DvmObject obj = vm.resolveClass("com/dta/lesson2/MainActivity").newObject(null);
        String data = "123456";
        DvmObject dvmObject = obj.callJniMethodObject(emulator, "md52([B)Ljava/lang/String;", data.getBytes(StandardCharsets.UTF_8));
        String result = (String) dvmObject.getValue();
        System.out.println("[symble] Call the so md5 function result is ==> "+ result);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainJni mainActivity = new MainJni();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.callMd5();
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/security/MessageDigest->update([B)V")){
            MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
            int intArg = vaList.getIntArg(0);
            Object object = vm.getObject(intArg).getValue();
            messageDigest.update((byte[]) object);
            return;
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/security/MessageDigest->digest()[B")){
            MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
            byte[] digest = messageDigest.digest();
            DvmObject<?> object = ProxyDvmObject.createObject(vm, digest);
            vm.addLocalObject(object);
            return object;
        }
        if (signature.equals("com/dta/lesson2/MainActivity->byte2Hex([B)Ljava/lang/String;")){
            int intArg = vaList.getIntArg(0);
            Object object = vm.getObject(intArg).getValue();
            String s = byte2Hex((byte[]) object);
            StringObject stringObject = new StringObject(vm, s);
            vm.addLocalObject(stringObject);
            return stringObject;
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public String byte2Hex(byte[] data){
        StringBuilder sb = new StringBuilder();
        for (byte b : data){
            String s = Integer.toHexString(b & 0xFF);
            if (s.length() < 2){
                sb.append("0");
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
