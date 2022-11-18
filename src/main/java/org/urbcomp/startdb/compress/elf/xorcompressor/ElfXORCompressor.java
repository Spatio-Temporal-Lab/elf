package org.urbcomp.startdb.compress.elf.xorcompressor;

import gr.aueb.delorean.chimp.OutputBitStream;

import java.util.Arrays;

public class ElfXORCompressor {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private final long[] storedValues;
    private boolean first = true;
    private int size;
    private final static long END_SIGN = 0x0000000000000001L;
    public final static short[] leadingRepresentation =
                    {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7,
                                    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                                    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};

    public final static short[] leadingRound =
                    {0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 8, 8, 12, 12, 12, 12, 16, 16, 18, 18, 20, 20, 22,
                                    22, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
                                    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
                                    24, 24, 24, 24, 24, 24, 24, 24, 24};

    private final OutputBitStream out;
    private final int previousValues;

    private final int previousValuesLog2;

    private final long setLsb;

    private final int shiftCount;
    private final int[] indices;
    private int index = 0;
    private int current = 0;

    // We should have access to the series?
    public ElfXORCompressor(int previousValues) {
        out = new OutputBitStream(
                        new byte[8125]); // for elf, we need one more bit for each at the worst case
        size = 0;
        this.previousValues = previousValues;
        this.previousValuesLog2 = (int) (Math.log(previousValues) / Math.log(2));
        int threshold = 6 + previousValuesLog2;
        this.shiftCount = 64 - threshold - 1;
        this.setLsb = ((long) Math.pow(2, threshold + 1) - 1) << shiftCount;
        this.indices = new int[(int) Math.pow(2, threshold + 1)];
        Arrays.fill(this.indices, -1);
        this.storedValues = new long[previousValues];
    }

    public OutputBitStream getOutputStream() {
        return out;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(long value) {
        if (first) {
            return writeFirst(value);
        } else {
            return compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(double value) {
        if (first) {
            return writeFirst(Double.doubleToRawLongBits(value));
        } else {
            return compressValue(Double.doubleToRawLongBits(value));
        }
    }

    private int writeFirst(long value) {
        first = false;
        storedValues[current] = value;
        int trailingZeros = Long.numberOfTrailingZeros(value);
        out.writeInt(trailingZeros, 6);
        out.writeLong(storedValues[current] >>> trailingZeros, 64 - trailingZeros);
        int key = (int) ((value & setLsb) >>> shiftCount);
        indices[key] = index;
        size += 70 - trailingZeros;
        return 70 - trailingZeros;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(long value) {
        int thisSize = 0;
        int key = (int) ((value & setLsb) >>> shiftCount);
        int m = indices[key];

        long xor;

        if (m != -1 && (index - m) < previousValues) {
            m = m % previousValues;
            xor = value ^ storedValues[m];
            if (xor == 0) {
                // case 00
                out.writeInt(m, previousValuesLog2 + 2);

                size += previousValuesLog2 + 2;
                thisSize += previousValuesLog2 + 2;
            } else {
                // case 01
                int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
                int trailingZeros = Long.numberOfTrailingZeros(xor);
                int centerBits = 64 - leadingZeros - trailingZeros;

                out.writeInt(1, 2);
                out.writeInt(m, previousValuesLog2);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeInt(centerBits, 6);
                out.writeLong(xor >>> trailingZeros, centerBits);

                size += 2 + previousValuesLog2 + 3 + 6 + centerBits;
                thisSize += 2 + previousValuesLog2 + 3 + 6 + centerBits;
            }
            storedLeadingZeros = 65;
        } else {
            m = index % previousValues;
            xor = value ^ storedValues[m];
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            int trailingZeros = Long.numberOfTrailingZeros(xor);
            int centerBits = 64 - leadingZeros - trailingZeros;

            if(leadingZeros == storedLeadingZeros) {
                // case 10
                out.writeInt(2, 2);
                out.writeInt(centerBits, 6);
                out.writeLong(xor >>> trailingZeros, centerBits);

                size += 2 + 6 + centerBits;
                thisSize += 2 + 6 + centerBits;
            } else {
                // case 11
                out.writeInt(3, 2);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeInt(centerBits, 6);
                out.writeLong(xor >>> trailingZeros, centerBits);

                size += 2 + 3 + 6 + centerBits;
                thisSize += 2 + 3 + 6 + centerBits;

                storedLeadingZeros = leadingZeros;
            }
        }

        current = (current + 1) % previousValues;
        storedValues[current] = value;
        index++;
        indices[key] = index;
        return thisSize;
    }

    public int getSize() {
        return size;
    }

}
