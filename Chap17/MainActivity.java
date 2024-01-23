package com.dta.lesson26;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;


public class MainActivity  {
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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson26/libnative-lib.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    static {
        Logger.getLogger(AndroidElfLoader.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.patch();
        mainActivity.check();

    }

    private void patch() {
        long patchAddr = module.base + 0xB1A;
        Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb);
        KeystoneEncoded assemble = keystone.assemble(
                "mov r11, r5\n" +
                        "nop\n" +
                        "nop\n" +
                        "nop\n" +
                        "nop\n" +
                        "nop");
        byte[] machineCode = assemble.getMachineCode();
        UnidbgPointer.pointer(emulator, patchAddr).write(machineCode);
    }

    private void check() {
//        emulator.attach().addBreakPoint(module, 0xB1A, new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                UnidbgPointer pointer = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
//                String string = pointer.getString(0);
//                System.out.println("code: "+string);
//                return true;
//            }
//        });
        //emulator.attach().addBreakPoint(module, 0xB18);


        DvmObject<?> obj = vm.resolveClass("com/r0ysue/crackme/MainActivity").newObject(null);
        String uname = "123123";
        String code = "";
        long b = obj.callJniMethodInt(emulator, "check([B[B)Z", uname.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
        System.out.println("result ==> "+ UnidbgPointer.pointer(emulator,b & 0xffffffffL).getString(0));
    }

}
