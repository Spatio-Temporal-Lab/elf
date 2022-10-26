package gr.aueb.delorean.chimp;

import gr.aueb.delorean.chimp.PmcMR.Constant;

import java.util.List;

public class DecompressorPmcMr {

	private List<Constant> constants;
    private float storedVal = 0f;
    private boolean endOfStream = false;
    private int currentElement = 0;
    private int currentTimestampOffset = 0;
    
    public DecompressorPmcMr(List<Constant> constants) {
    	this.constants = constants;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Float readValue() {
        next();
        if(endOfStream) {
            return null;
        }
        return storedVal;
    }

    private void next() {
    	Constant constant = constants.get(currentElement);
    	if (constant.getFinalTimestamp() >= (constant.getInitialTimestamp() + currentTimestampOffset)) {
    		storedVal = constant.getValue();
    		currentTimestampOffset++;
    	} else {
    		currentElement++;
    		if (currentElement < constants.size()) {
    			constant = constants.get(currentElement);
    			storedVal = constant.getValue();
    			currentTimestampOffset = 1;
    		} else {
    			endOfStream = true;
    		}
    	}
	}

}