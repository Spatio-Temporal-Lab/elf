package gr.aueb.delorean.chimp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PmcMR {

	
	public List<Constant> filter(Collection<Point> points, float epsilon) {

		List<Constant> constants = new ArrayList<>();
		Constant currentConstant = null;
		
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		
		for (Point point : points) {
			
			if (point.getValue() > max) {
				max = point.getValue();
			}
			if (point.getValue() < min) {
				min = point.getValue();
			}
			
			if (max - min <= epsilon && currentConstant != null) {
				currentConstant.setFinalTimestamp(point.getTimestamp());
				currentConstant.setValue(max - ((max - min) / 2));
			} else {
				if (currentConstant != null) {
					constants.add(currentConstant);	
				}
				max = point.getValue();
				min = point.getValue();
				currentConstant = new Constant();
				currentConstant.setInitialTimestamp(point.getTimestamp());
				currentConstant.setFinalTimestamp(point.getTimestamp());
				currentConstant.setValue(point.getValue());
			}
			
		}
		if (currentConstant != null) {
			constants.add(currentConstant);	
		}
		
		return constants;
	}
	
	public class Constant {
		
		private long initialTimestamp;
		private long finalTimestamp;
		private float value;
		
		public void setFinalTimestamp(long finalTimestamp) {
			this.finalTimestamp = finalTimestamp;
		}
		
		public void setInitialTimestamp(long initialTimestamp) {
			this.initialTimestamp = initialTimestamp;
		}
		
		public void setValue(float value) {
			this.value = value;
		}
		
		public long getFinalTimestamp() {
			return finalTimestamp;
		}
		
		public long getInitialTimestamp() {
			return initialTimestamp;
		}
		
		public float getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			return String.format("%d-%d: %f", getInitialTimestamp(), getFinalTimestamp(), getValue());
		}
		
	}
	
}
