package fi.iki.yak.ts.compression.gorilla;

import gr.aueb.delorean.chimp.InputBitStream;
import org.apache.commons.lang.NotImplementedException;

import javax.management.RuntimeErrorException;
import java.io.IOException;

/**
 * An implementation of BitInput that parses the data from byte array or existing ByteBuffer.
 *
 * @author Michael Burman
 */
public class ByteBufferBitInput2 implements BitInput {
    private InputBitStream ibs;

    /**
     * Uses an existing ByteBuffer to read the stream. Starts at the ByteBuffer's current position.
     *
     * @param buf Use existing ByteBuffer
     */
    public ByteBufferBitInput2(InputBitStream ibs) {
        this.ibs = ibs;
    }

    /**
     * Reads the next bit and returns a boolean representing it.
     *
     * @return true if the next bit is 1, otherwise 0.
     */
    @Override
	public boolean readBit() {
    	try {
			return ibs.readBit() == 1;
		} catch (IOException e) {
			return false;
		}
    }

    public int getInt(int bits) {
    	try {
			return ibs.readInt(bits);
		} catch (IOException e) {
			throw new RuntimeErrorException(null, e.getMessage());
		}
    }

    /**
     * Reads a long from the next X bits that represent the least significant bits in the long value.
     *
     * @param bits How many next bits are read from the stream
     * @return long value that was read from the stream
     */
    @Override
	public long getLong(int bits) {
        try {
			return ibs.readLong(bits);
		} catch (IOException e) {
			throw new RuntimeErrorException(null, e.getMessage());
		}
    }

    @Override
    public int nextClearBit(int maxBits) {
        throw new NotImplementedException();
    }

    /**
     * Returns the underlying ByteBuffer
     *
     * @return ByteBuffer that's connected to the underlying stream
     */
    public InputBitStream getByteBuffer() {
        return this.ibs;
    }
}
