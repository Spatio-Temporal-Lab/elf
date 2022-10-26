package gr.aueb.delorean.chimp;
public class SwingSegment {

	private long initialTimestamp;
	private long finalTimestamp;
	private LinearFunction line;

	public SwingSegment(long initialTimestamp, long finalTimestamp, LinearFunction line) {
		this.initialTimestamp = initialTimestamp;
		this.finalTimestamp = finalTimestamp;
		this.line = line;
	}

	public long getFinalTimestamp() {
		return finalTimestamp;
	}

	public long getInitialTimestamp() {
		return initialTimestamp;
	}

	public LinearFunction getLine() {
		return line;
	}

	@Override
	public String toString() {
		return String.format("%d-%d: %f", getInitialTimestamp(), getFinalTimestamp(), getLine());
	}

}