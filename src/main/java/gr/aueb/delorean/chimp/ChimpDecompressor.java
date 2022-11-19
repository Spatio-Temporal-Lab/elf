package gr.aueb.delorean.chimp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 */
public class ChimpDecompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public ChimpDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public List<Double> getValues() {
        List<Double> list = new LinkedList<>();
        Double value = readValue();
        while (value != null) {
            list.add(value);
            value = readValue();
        }
        return list;
    }

    public InputBitStream getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if(endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.readLong(64);
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {

        int significantBits;
        long value;
        // Read value
        int flag = in.readInt(2);
        switch(flag) {
            case 3:
                // New leading zeros
                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
                significantBits = 64 - storedLeadingZeros;
                if(significantBits == 0) {
                    significantBits = 64;
                }
                value = in.readLong(64 - storedLeadingZeros);
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
                break;
            case 2:
                significantBits = 64 - storedLeadingZeros;
                if(significantBits == 0) {
                    significantBits = 64;
                }
                value = in.readLong(64 - storedLeadingZeros);
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
                break;
            case 1:
                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
                significantBits = in.readInt(6);
                if(significantBits == 0) {
                    significantBits = 64;
                }
                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
                value = in.readLong(64 - storedLeadingZeros - storedTrailingZeros);
                value <<= storedTrailingZeros;
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
                break;
            default:
        }
    }

}
