package gr.aueb.delorean.chimp;

import java.math.BigDecimal;

public class SwingSegmentBigDecimal {

	private long initialTimestamp;
	private long finalTimestamp;
	private LinearFunctionBigDecimal line;

	public SwingSegmentBigDecimal(long initialTimestamp, long finalTimestamp, LinearFunctionBigDecimal line) {
		this.initialTimestamp = initialTimestamp;
		this.finalTimestamp = finalTimestamp;
		this.line = line;
	}

    public SwingSegmentBigDecimal(long initialTimestamp, long finalTimestamp, BigDecimal first, BigDecimal last) {
        this.initialTimestamp = initialTimestamp;
        this.finalTimestamp = finalTimestamp;
        this.line = new LinearFunctionBigDecimal(initialTimestamp, first, finalTimestamp + 1, last);
    }

	public long getFinalTimestamp() {
		return finalTimestamp;
	}

	public long getInitialTimestamp() {
		return initialTimestamp;
	}

	public LinearFunctionBigDecimal getLine() {
		return line;
	}

	@Override
	public String toString() {
		return String.format("%d-%d: %f", getInitialTimestamp(), getFinalTimestamp(), getLine());
	}

}