package gr.aueb.delorean.chimp;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Decompressor32OS {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static int END_SIGN = Float.floatToIntBits(Float.NaN);

    public Decompressor32OS(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public InputBitStream getInputStream() {
        return in;
    }

    public List<Float> getValues() {
        List<Float> list = new LinkedList<>();
        Value value = readValue();
        while (value != null) {
            list.add(value.getFloatValue());
            value = readValue();
        }
        return list;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.readInt(32);
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        // Read value
        if (in.readBit() == 1) {
            // else -> same value as before
            if (in.readBit() == 1) {
                // New leading and trailing zeros
                storedLeadingZeros = in.readInt(4);

                int significantBits = in.readInt(5);
                if(significantBits == 0) {
                    significantBits = 32;
                }
                storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
            }
            int value = in.readInt(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }

        }
    }
}
