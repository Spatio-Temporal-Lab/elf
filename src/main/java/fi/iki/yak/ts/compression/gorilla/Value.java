package fi.iki.yak.ts.compression.gorilla;

/**
 * Value is an extracted value from the stream
 *
 * @author Michael Burman
 */
public class Value {
    private long value;

    public Value(long value) {
        this.value = value;
    }

    public double getDoubleValue() {
        return Double.longBitsToDouble(value);
    }

    public long getLongValue() {
        return value;
    }
}
