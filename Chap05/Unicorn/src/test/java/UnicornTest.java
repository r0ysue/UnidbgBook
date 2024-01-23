import capstone.Capstone;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneMode;
import org.junit.Test;
import unicorn.*;

import static unicorn.UnicornConst.UC_ERR_READ_UNMAPPED;
import static unicorn.UnicornConst.UC_HOOK_MEM_READ_UNMAPPED;

public class UnicornTest {

    long BASE = 0x1000;

    @Test
    public void test(){
        Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb);
        String assembly =
                "movs r0, #3\n" +
                "movs r1, #2\n" +
                "add r0,r1\n" +
                "movs r2, #0x1100\n" +
                "str r0, [r2, #0]\n" +
                "ldr r3, [r2, #0]";
        byte[] code = keystone.assemble(assembly).getMachineCode();

        Capstone cs = new Capstone(Capstone.CS_ARCH_ARM,Capstone.CS_MODE_THUMB);
        Capstone.CsInsn[] disasm = cs.disasm(code, 0x1000);
        for (Capstone.CsInsn i : disasm){
            System.out.println(String.format("0x%x:%s %s",i.address,i.mnemonic,i.opStr));
        }

//        byte[] code = new byte[]{0x03,0x20,
//                0x02,0x21,
//                0x08,0x44};

        Unicorn unicorn = new Unicorn(UnicornConst.UC_ARCH_ARM,UnicornConst.UC_MODE_THUMB);

        unicorn.mem_map(BASE,0x1000,UnicornConst.UC_PROT_WRITE | UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_EXEC);

        unicorn.mem_write(BASE,code);

        unicorn.hook_add(new CodeHook() {
            @Override
            public void hook(Unicorn u, long address, int size, Object user) {
                System.out.print(String.format(">>> Tracing instruction at 0x%x, instruction size = 0x%x\n", address, size));
            }
        },0,-1,null);

        unicorn.hook_add(new BlockHook() {
            @Override
            public void hook(Unicorn u, long address, int size, Object user) {
                System.out.print(String.format(">>> Tracing basic block at 0x%x, block size = 0x%x\n", address, size));
            }
        },BASE,BASE+code.length,null);

        unicorn.hook_add(new ReadHook() {
            @Override
            public void hook(Unicorn u, long address, int size, Object user) {
                byte[] bytes = u.mem_read(address, size);
                System.out.print(String.format(">>> Memory read at 0x%x, block size = 0x%x, value is = 0x%s\n", address, size, Integer.toHexString(bytes[0] & 0xff)));
            }
        },BASE+0x100,BASE+0x102,null);

        unicorn.hook_add(new WriteHook() {
            @Override
            public void hook(Unicorn u, long address, int size, long value, Object user) {
                System.out.print(String.format(">>> Memory write at 0x%x, block size = 0x%x, value is = 0x%x\n", address, size,value));
            }
        },BASE+0x100,BASE+0x102,null);

        unicorn.hook_add(new EventMemHook() {
            @Override
            public boolean hook(Unicorn u, long address, int size, long value, Object user) {
                System.out.println("Event");
                return false;
            }
        },UC_ERR_READ_UNMAPPED,null);

//        unicorn.hook_add(new MemHook() {
//            @Override
//            public void hook(Unicorn u, long address, int size, Object user) {
//
//            }
//
//            @Override
//            public void hook(Unicorn u, long address, int size, long value, Object user) {
//
//            }
//        });

        unicorn.emu_start(BASE+1,BASE+code.length,0,0);

        Long o = (Long) unicorn.reg_read(ArmConst.UC_ARM_REG_R0);

        System.out.println("the emulate finished result is ==> "+o.intValue());


    }
}
