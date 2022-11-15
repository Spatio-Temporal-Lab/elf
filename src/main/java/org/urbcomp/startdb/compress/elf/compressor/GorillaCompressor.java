package org.urbcomp.startdb.compress.elf.compressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import fi.iki.yak.ts.compression.gorilla.Compressor;

import java.nio.ByteBuffer;

public class GorillaCompressor implements ICompressor {
    private final Compressor gorilla;

    public GorillaCompressor() {
        this.gorilla = new Compressor(new ByteBufferBitOutput());
    }

    @Override public void addValue(double v) {
        gorilla.addValue(v);
    }

    @Override public int getSize() {
        return gorilla.getSize();
    }

    @Override public byte[] getBytes() {
        ByteBuffer byteBuffer = ((ByteBufferBitOutput) gorilla.getOutputStream()).getByteBuffer();
        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    @Override public void close() {
        gorilla.close();
    }
}
