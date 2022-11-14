package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.AbstractElfCompressor;
import org.urbcomp.startdb.compress.elf.compressor.ElfOnGorillaCompressor;

public class Main {
    public static void main(String[] args){
        AbstractElfCompressor compressor = new ElfOnGorillaCompressor();
        compressor.addValue(3.14);
        compressor.addValue(3.15);
        compressor.addValue(3.16);
        System.out.println(compressor.getSize());
    }
}
