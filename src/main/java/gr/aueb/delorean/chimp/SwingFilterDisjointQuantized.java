package gr.aueb.delorean.chimp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SwingFilterDisjointQuantized {

	public List<SwingSegment> filter(List<Point> points, float epsilon) {
		List<SwingSegment> swingSegments = new ArrayList<>();
		List<Integer> recordings = new ArrayList<>();
		double aMin = Double.MIN_VALUE;
		double aMax = Double.MAX_VALUE;

		int idx = 0;
		for (Point point: points) {
			float upValue = point.getValue() + epsilon;
			float downValue = point.getValue() - epsilon;

			if (recordings.isEmpty()) {
				aMin = Double.MIN_VALUE;
				aMax = Double.MAX_VALUE;
				recordings.add(idx++);
			} else {
				float b = quantizationFunc(points.get(recordings.get(0)).getValue(), epsilon);
				if (recordings.size() > 1) {
					double upLim = aMax * (idx - recordings.get(0)) + b;
					double downLim = aMin * (idx - recordings.get(0)) + b;
					if (downValue > upLim || upValue < downLim) {
						swingSegments.add(new SwingSegmentDisjointQuantized(recordings.get(0), idx - 1, aMin, aMax, b));
						recordings.clear();
						recordings.add(idx++);
						continue;
					}
				}

				float aMaxTemp = (upValue - quantizationFunc(points.get(recordings.get(0)).getValue(), epsilon)) / (idx - recordings.get(0));
				float aMinTemp = (downValue - quantizationFunc(points.get(recordings.get(0)).getValue(), epsilon)) / (idx - recordings.get(0));

				if (recordings.size() == 1) {
					aMax = aMaxTemp;
					aMin = aMinTemp;
				} else {
					double upLim = aMax * (idx - recordings.get(0)) + b;
					double downLim = aMin * (idx - recordings.get(0)) + b;
					if (upValue < upLim) {
						aMax = aMaxTemp >= aMin ? aMaxTemp : aMin;
					}
					if (downValue > downLim) {
						aMin = aMinTemp <= aMax ? aMinTemp : aMax;
					}
				}
				recordings.add(idx++);
			}
		}
		if (recordings.size() >= 0) {
			float b = quantizationFunc(points.get(recordings.get(0)).getValue(), epsilon);
			swingSegments.add(new SwingSegmentDisjointQuantized(recordings.get(0), idx - 1, aMin, aMax, b));
		}
		return swingSegments;
	}


	private float quantizationFunc(float f, float epsilon) {
		return ((int) (f / epsilon)) * epsilon;
	}


	public List<SwingSegment> filter2(Collection<Point> points, float epsilon) {

		List<SwingSegment> swingSegments = new ArrayList<>();

		Point first = null;
		LinearFunction uiOld = null;
		LinearFunction liOld = null;
		Point last = null;

		for (Point point : points) {
			last = point;
			if (first == null) {
				first = point;
			}
			else {
				if (uiOld != null && liOld !=null && (uiOld.get(point.getTimestamp()) < point.getValue() || liOld.get(point.getTimestamp()) > point.getValue())) {
					LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(), point.getTimestamp(), (uiOld.get(point.getTimestamp()) + liOld.get(point.getTimestamp())) / 2);
//					System.out.println("need to start new line: " + line.toString() + " : " + first.getTimestamp() + " " + (point.getTimestamp() - 1));
					swingSegments.add(new SwingSegment(first.getTimestamp(), point.getTimestamp() - 1, line));
					uiOld = null;
					liOld = null;
					first = point;
				} else {
					LinearFunction uiNew = new LinearFunction(first.getTimestamp(), first.getValue(), point.getTimestamp(), point.getValue() + epsilon);
					LinearFunction liNew = new LinearFunction(first.getTimestamp(), first.getValue(), point.getTimestamp(), point.getValue() - epsilon);

					if (uiOld == null || uiOld.get(point.getTimestamp()) > uiNew.get(point.getTimestamp())) {
						uiOld = uiNew;
//						System.out.println("resetting upper: " + uiOld);
					}
					if (liOld == null || liOld.get(point.getTimestamp()) < liNew.get(point.getTimestamp())) {
						liOld = liNew;
//						System.out.println("resetting lower: " + liOld);
					}
				}
			}
		}

		if (uiOld != null && liOld !=null) {
//			System.out.println("need to start new line");
			LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(), last.getTimestamp(), (uiOld.get(last.getTimestamp()) + liOld.get(last.getTimestamp())) / 2);
			swingSegments.add(new SwingSegment(first.getTimestamp(), last.getTimestamp(), line));
		} else {
			LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(), first.getTimestamp() + 1, first.getValue());
			swingSegments.add(new SwingSegment(first.getTimestamp(), first.getTimestamp(), line));
		}

		return swingSegments;
	}



	/**
	 *
	 *
	 * // initialization
1. (t1,X1) = getNext();(t2,X2) = getNext();
2. Make a recording: (t0’,X0’) = (t1,X1);
3. Start a new filtering interval with ui1 passing through (t1,X1)
and (t2,X2+Vd(i,εi)); and li1 passing through (t1,X1) and (t2,X2-
Vd(i,εi)), for every dimension xi, i∈[1,d];
4. set k = 1;
//main loop
5. while (true)
6. (tj,Xj) = getNext();
7. if (tj,Xj) is null or (tj,Xj) is more than εi above uik or below lik
in the xi dimension for any i∈[1,d] //recording mechanism
8. Make a new recording: (tk,Xk), such that tk=tj-1, xik falls
between uik and lik, and xik minimizes Eik, for every
dimension xi, i∈[1,d];
9. Start a new filtering interval with ui(k+1) passing through
(tk,Xk) and (tj,Xj+Vd(i,εi)); and li(k+1) passing through (tk,xk)
and (tj,Xj-Vd(i,εi));
10. set k = k+1;
11. if (tj,Xj) is null //end of signal
12. return;
13. else //filtering mechanism
14. for each dimension xi, i∈[1,d]
15. if (tj,Xj) falls more than εi above lik in the xi dimension
16. “Swing up” lik such that it passes through (tk,xk) and
(tj,Xj-Vd(i,εi));
17. if (tj,Xj) falls more than εi below uik in the xi dimension
18. “Swing down” uik such that it passes through (tk,xk)
and (tj,Xj+Vd(i,εi));
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
	 *
    lines = []
    line_first_timestamp, line_first_value = None, None
    coefficients_up, coefficients_down = None, None
    polynomial_up, polynomial_down = None, None
    for timestamp, value in ts.items():
        if polynomial_up is not None and polynomial_down is not None:
            up_val = value + epsilon
            down_val = value - epsilon
            up_lim = polynomial_up(timestamp)
            down_lim = polynomial_down(timestamp)

            if (not math.isclose(up_val, up_lim) and up_val > up_lim and not math.isclose(down_val, up_lim) and down_val > up_lim) or\
                    (not math.isclose(up_val, down_lim) and up_val < down_lim and not math.isclose(down_val, down_lim) and down_val < down_lim):
                lines.append([line_first_timestamp, np.polyfit(x=[line_first_timestamp, previous_timestamp], y=[line_first_value, previous_value], deg=1)])
                line_first_timestamp, line_first_value = None, None
                coefficients_up, coefficients_down = None, None
                polynomial_up, polynomial_down = None, None

        if line_first_timestamp is None and line_first_value is None:
            line_first_timestamp, line_first_value = timestamp, value
            continue

        coefficients_up_temp = np.polyfit(x=[line_first_timestamp, timestamp], y=[line_first_value, value + epsilon], deg=1)
        coefficients_down_temp = np.polyfit(x=[line_first_timestamp, timestamp], y=[line_first_value, value - epsilon], deg=1)
        polynomial_up_temp = np.poly1d(coefficients_up_temp)
        polynomial_down_temp = np.poly1d(coefficients_down_temp)

        if coefficients_up is None or coefficients_down is None:
            coefficients_up = coefficients_up_temp
            coefficients_down = coefficients_down_temp
            polynomial_up = np.poly1d(coefficients_up)
            polynomial_down = np.poly1d(coefficients_down)
        if polynomial_up_temp(timestamp) < polynomial_up(timestamp):
            coefficients_up = coefficients_up_temp
            polynomial_up = np.poly1d(coefficients_up)
        if polynomial_down_temp(timestamp) > polynomial_down(timestamp):
            coefficients_down = coefficients_down_temp
            polynomial_down = np.poly1d(coefficients_down)
        previous_timestamp = timestamp
        previous_value = value

    # Raises a warning if there is one point only, line_first_timestamp == timestamp
    lines.append([line_first_timestamp, np.polyfit(x=[line_first_timestamp, timestamp], y=[line_first_value, value], deg=1)])

	 *
	 * */

}
