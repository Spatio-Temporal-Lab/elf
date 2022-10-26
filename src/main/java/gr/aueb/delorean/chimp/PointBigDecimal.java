package gr.aueb.delorean.chimp;

import java.math.BigDecimal;

public class PointBigDecimal {

	private final long timestamp;
	private BigDecimal value;

	public PointBigDecimal(long timestamp, BigDecimal value) {
		this.timestamp = timestamp;
		this.value = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
        this.value = value;
    }

}
