package com.dta.lesson31;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class MainActivity2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity2(){
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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson31/libcheck.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    static {
        Logger.getLogger(AndroidElfLoader.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity = new MainActivity2();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.sub_85E0();
    }

    private void sub_85E0() {
        //emulator.traceCode();
        List<Object> args = new ArrayList<>();
        UnidbgPointer ptr_arg0 = UnidbgPointer.pointer(emulator, module.base + 0xF1B0);
        args.add(ptr_arg0.toIntPeer());
        args.add(622);

        MemoryBlock malloc = memory.malloc(32, true);
        UnidbgPointer ptr_md5 = malloc.getPointer();
        String md5 = "f8c49056e4ccf9a11e090eaf471f418d";
        ptr_md5.write(md5.getBytes(StandardCharsets.UTF_8));
        args.add(ptr_md5.toIntPeer());

        Number[] numbers = module.callFunction(emulator, 0x85E1, args.toArray());
        System.out.println("result => " + numbers[0].longValue());

        sub_shellCode(numbers[0].longValue());
    }

    private void sub_shellCode(long addr) {
        List<Object> args = new ArrayList<>();

        String input = "qqqqqqq";
        MemoryBlock malloc = memory.malloc(input.length(), true);
        UnidbgPointer ptr_input = malloc.getPointer();

        UnidbgPointer ptr_v9 = memory.allocateStack(8);
        ptr_v9.setPointer(0,ptr_input);


        UnidbgPointer ptr_pipe = memory.allocateStack(8);
        ptr_pipe.setInt(0,0);
        ptr_pipe.setInt(4,1);

        ptr_v9.setPointer(4,ptr_pipe);

        args.add(ptr_v9.toIntPeer());
        Number[] numbers = module.callFunction(emulator, addr - module.base + 1, args.toArray());
        System.out.println("shellcode result => " + numbers[0].longValue());
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if(signature.equals("com/a/sample/loopcrypto/Decode->a([BI)Ljava/lang/String;")){
            byte[] bytes = (byte[]) varArg.getObjectArg(0).getValue();
            int i = varArg.getIntArg(1);
            String a = Encrypt.a(bytes, i);
            return new StringObject(vm, a);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }
}
