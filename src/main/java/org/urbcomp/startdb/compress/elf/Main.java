package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.*;

public class Main {
    public static void main(String[] args){
        ICompressor compressor = new ElfOnGorillaCompressor();
        compressor.addValue(3.14);
        compressor.addValue(3.15);
        compressor.addValue(3.16);
        compressor.addValue(3.17);
        compressor.addValue(3.18);
        compressor.close();
        System.out.println(compressor.getSize());
    }
}
