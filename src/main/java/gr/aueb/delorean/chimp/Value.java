package gr.aueb.delorean.chimp;

/**
 * Value is an extracted value from the stream
 *
 * @author Michael Burman
 */
public class Value {
    private int value;

    public Value(int value) {
        this.value = value;
    }

    public float getFloatValue() {
        return Float.intBitsToFloat(value);
    }

    public int getIntValue() {
        return value;
    }
}
