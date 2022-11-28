package gr.aueb.delorean.chimp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class ChimpNDecompressor32 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private int storedValues[];
    private int current = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;
    private int previousValues;
    private int previousValuesLog2;

    public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    private final static int NAN_INT = 0x7fc00000;

    public ChimpNDecompressor32(byte[] bs, int previousValues) {
        in = new InputBitStream(bs);
        this.previousValues = previousValues;
        this.previousValuesLog2 = (int) (Math.log(previousValues) / Math.log(2));
        this.storedValues = new int[previousValues];
    }

    public InputBitStream getInputStream() {
        return in;
    }

    public List<Float> getValues() {
        List<Float> list = new LinkedList<>();
        Float value = readValue();
        while (value != null) {
            list.add(value);
            value = readValue();
        }
        return list;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Float readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Float.intBitsToFloat(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.readInt(32);
            storedValues[current] = storedVal;
            if (storedValues[current] == NAN_INT) {
                endOfStream = true;
                return;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        if (in.readBit() == 1) {
            if (in.readBit() == 1) {
                // New leading zeros
                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
            } else {
            }
            int significantBits = 32 - storedLeadingZeros;
            if (significantBits == 0) {
                significantBits = 32;
            }
            int value = in.readInt(32 - storedLeadingZeros);
            value = storedVal ^ value;

            if (value == NAN_INT) {
                endOfStream = true;
                return;
            } else {
                storedVal = value;
                current = (current + 1) % previousValues;
                storedValues[current] = storedVal;
            }

        } else if (in.readBit() == 1) {
            int fill = previousValuesLog2 + 8;
            int temp = in.readInt(fill);
            int index = temp >>> (fill -= previousValuesLog2) & (1 << previousValuesLog2) - 1;
            storedLeadingZeros = leadingRepresentation[temp >>> (fill -= 3) & (1 << 3) - 1];
            int significantBits = temp >>> (fill -= 5) & (1 << 5) - 1;
            storedVal = storedValues[index];
            if (significantBits == 0) {
                significantBits = 32;
            }
            storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
            int value = in.readInt(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
                endOfStream = true;
                return;
            } else {
                storedVal = value;
                current = (current + 1) % previousValues;
                storedValues[current] = storedVal;
            }
        } else {
            // else -> same value as before
            int index = in.readInt(previousValuesLog2);
            storedVal = storedValues[index];
            current = (current + 1) % previousValues;
            storedValues[current] = storedVal;
        }
    }

}