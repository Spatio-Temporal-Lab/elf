package gr.aueb.delorean.chimp;

import java.math.BigDecimal;
import java.util.List;

public class DecompressorSwingFilterBigDecimal {

	private List<SwingSegmentBigDecimal> swingSegments;
    private BigDecimal storedVal = new BigDecimal(0);
    private boolean endOfStream = false;
    private int currentElement = 0;
    private int currentTimestampOffset = 0;

    public DecompressorSwingFilterBigDecimal(List<SwingSegmentBigDecimal> constants) {
    	this.swingSegments = constants;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public BigDecimal readValue() {
        next();
        if(endOfStream) {
            return null;
        }
        return storedVal;
    }

    private void next() {
    	SwingSegmentBigDecimal swingSegment = swingSegments.get(currentElement);
    	if (swingSegment.getFinalTimestamp() >= (swingSegment.getInitialTimestamp() + currentTimestampOffset)) {
    		storedVal = swingSegment.getLine().get(swingSegment.getInitialTimestamp() + currentTimestampOffset);
//    		System.out.println("LineDec: " + swingSegment.getLine() + "\t" + (swingSegment.getInitialTimestamp() + currentTimestampOffset) + "\t" + storedVal);
    		currentTimestampOffset++;
    	} else {
    		currentElement++;
    		if (currentElement < swingSegments.size()) {
    			swingSegment = swingSegments.get(currentElement);
    			storedVal = swingSegment.getLine().get(swingSegment.getInitialTimestamp());

    			currentTimestampOffset = 1;
    		} else {
    			endOfStream = true;
    		}
    	}
	}

}