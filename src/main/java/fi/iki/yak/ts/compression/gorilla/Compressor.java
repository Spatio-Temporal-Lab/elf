package fi.iki.yak.ts.compression.gorilla;

/**
 * Implements the time series compression as described in the Facebook's Gorilla Paper. Value compression
 * is for floating points only.
 *
 * @author Michael Burman
 */
public class  Compressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private int perSize;
    private int[] flag;

//    public final static short FIRST_DELTA_BITS = 27;

    private BitOutput out;

    // We should have access to the series?
    public Compressor(BitOutput output) {
        out = output;
        size = 0;
        flag = new int[4];
    }

    public Compressor(BitOutput output,int size){
        out = output;
        this.size = size;
        flag = new int[4];
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(long value) {
        perSize=0;
        if(first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
        size+=perSize;
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(double value) {
        perSize=0;
        if(first) {
            writeFirst(Double.doubleToRawLongBits(value));
        } else {
            compressValue(Double.doubleToRawLongBits(value));
        }
        size+=perSize;
    }

    private void writeFirst(long value) {
    	first = false;
        storedVal = value;
        out.writeBits(storedVal, 64);
        perSize += 64;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
    	addValue(Double.NaN);
        out.skipBit();
        out.flush();
    }

    private void compressValue(long value) {
        // TODO Fix already compiled into a big method
       long xor = storedVal ^ value;

        if(xor == 0) {
            // Write 0
            out.skipBit();
            perSize += 1;
            flag[0]++;
        } else {
            int leadingZeros = Long.numberOfLeadingZeros(xor);
            int trailingZeros = Long.numberOfTrailingZeros(xor);

            // Check overflow of leading? Can't be 32!
            if(leadingZeros >= 32) {
                leadingZeros = 31;
            }

            // Store bit '1'
            out.writeBit();
            perSize += 1;

            if(leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros) {
                writeExistingLeading(xor);
                flag[2]++;
            } else {
                writeNewLeading(xor, leadingZeros, trailingZeros);
                flag[3]++;
            }
        }

        storedVal = value;
    }

    /**
     * If there at least as many leading zeros and as many trailing zeros as previous value, control bit = 0 (type a)
     * store the meaningful XORed value
     *
     * @param xor XOR between previous value and current
     */
    private void writeExistingLeading(long xor) {
        out.skipBit();
        int significantBits = 64 - storedLeadingZeros - storedTrailingZeros;
        out.writeBits(xor >>> storedTrailingZeros, significantBits);
        perSize += 1 + significantBits;
    }

    public int[] getFlag() {
        return flag;
    }

    /**
     * store the length of the number of leading zeros in the next 5 bits
     * store length of the meaningful XORed value in the next 6 bits,
     * store the meaningful bits of the XORed value
     * (type b)
     *
     * @param xor XOR between previous value and current
     * @param leadingZeros New leading zeros
     * @param trailingZeros New trailing zeros
     */
    private void writeNewLeading(long xor, int leadingZeros, int trailingZeros) {
        out.writeBit();
        out.writeBits(leadingZeros, 5); // Number of leading zeros in the next 5 bits

        int significantBits = 64 - leadingZeros - trailingZeros;
        if (significantBits == 64) {
        	out.writeBits(0, 6); // Length of meaningful bits in the next 6 bits	
        } else {
        	out.writeBits(significantBits, 6); // Length of meaningful bits in the next 6 bits
        }

        out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;

        perSize += 1 + 5 + 6 + significantBits;
    }

    public int getSize() {
    	return size;
    }

    public int getPerSize() {
        return perSize;
    }
}
