package com.dta.lesson32;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson32/libdogpro.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }



    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.getHash();
    }

    private void getHash() {
        DvmObject<?> dvmObject = vm.resolveClass("com/example/dogpro/MainActivity").newObject(null);
        //public native String getHash(String str);
        ///data/app/com.example.dogpro-pnF2J3-qBi8ei74vXTNXmQ==/base.apk
        String input = "/data/app/com.example.dogpro-pnF2J3-qBi8ei74vXTNXmQ==/base.apk";
        DvmObject<?> ret = dvmObject.callJniMethodObject(emulator, "getHash(Ljava/lang/String;)Ljava/lang/String;", input);
        System.out.println("result ==> "+ret.getValue());
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if (signature.equals("java/util/zip/ZipFile-><init>(Ljava/lang/String;)V")){
            String name = (String) vaList.getObjectArg(0).getValue();
            try {
                if (name.equals("/data/app/com.example.dogpro-pnF2J3-qBi8ei74vXTNXmQ==/base.apk")){
                    ZipFile zipFile = new ZipFile("unidbg-android/src/test/java/com/dta/lesson32/app-debug.apk");
                    return vm.resolveClass("java/util/zip/ZipFile").newObject(zipFile);
                }
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
//        if (signature.equals("java/util/zip/ZipFile->entries()Ljava/util/Enumeration;")){
//            ZipFile zipFile = (ZipFile) dvmObject.getValue();
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            DvmClass ZipEntryClass = vm.resolveClass("java/util/zip/ZipEntry");
//            List<DvmObject<?>> objs = new ArrayList<>();
//            while (entries.hasMoreElements()){
//                ZipEntry zipEntry = entries.nextElement();
//                objs.add(ZipEntryClass.newObject(zipEntry));
//            }
//            return new com.github.unidbg.linux.android.dvm.Enumeration(vm,objs);
//        }
        if (signature.equals("java/util/zip/ZipEntry->getName()Ljava/lang/String;")){
            ZipEntry zipEntry = (ZipEntry) dvmObject.getValue();
            String name = zipEntry.getName();
            return new  StringObject(vm,name);
        }
        if (signature.equals("java/lang/String->toLowerCase()Ljava/lang/String;")){
            String s = (String) dvmObject.getValue();
            String s1 = s.toLowerCase();
            return new StringObject(vm,s1);
        }
        if (signature.equals("java/util/zip/ZipFile->getInputStream(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;")){
            ZipFile zipFile = (ZipFile) dvmObject.getValue();
            ZipEntry zipEntry = (ZipEntry) vaList.getObjectArg(0).getValue();
            try {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                return vm.resolveClass("java/io/InputStream").newObject(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        if (signature.equals("java/security/MessageDigest->digest()[B")){
            MessageDigest md = (MessageDigest) dvmObject.getValue();
            byte[] digest = md.digest();
            return new ByteArray(vm, digest);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/lang/String->endsWith(Ljava/lang/String;)Z")){
            String value = (String) dvmObject.getValue();
            String suffix = (String) vaList.getObjectArg(0).getValue();
            return value.endsWith(suffix);
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/io/InputStream->read([B)I")){
            InputStream inputStream = (InputStream) dvmObject.getValue();
            byte[] bytes = (byte[]) vaList.getObjectArg(0).getValue();
            try {
                int read = inputStream.read(bytes);
                return read;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/security/MessageDigest->update([B)V")){
            MessageDigest md = (MessageDigest) dvmObject.getValue();
            byte[] bytes = (byte[]) vaList.getObjectArg(0).getValue();
            md.update(bytes);
            return;
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }
}
