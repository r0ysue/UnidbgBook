import com.bxlong.elf.ElfFile;
import com.bxlong.elf.ElfSegment;

import java.io.File;
import java.io.IOException;

public class ElfParserTest {
    public static void main(String[] args) throws IOException {
        ElfFile elf = ElfFile.from(new File(ElfParserTest.class.getClassLoader().getResource("example/liboasiscore.so").getFile()));
        ElfSegment dynamicSegment = elf.getDynamicSegment();
        //dynamicSegment.getDynamicStructure().getSymbolTable().getValue().getELFSymbolByAddr(0);
        //dynamicSegment.getDynamicStructure().getSymbolTable().getValue().getELFSymbolByName("free");
        //System.out.println(dynamicSegment.type);
    }
}
