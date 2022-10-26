package gr.aueb.delorean.chimp;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SwingFilterBigDecimal {

    MathContext mc = new MathContext(30, RoundingMode.HALF_UP) ;

    public List<SwingSegmentBigDecimal> filter(Collection<PointBigDecimal> points, BigDecimal epsilon) {

        List<SwingSegmentBigDecimal> swingSegments = new ArrayList<>();

        PointBigDecimal first = null;
        LinearFunctionBigDecimal uiOld = null;
        LinearFunctionBigDecimal liOld = null;

        Iterator<PointBigDecimal> iterator = points.iterator();

        PointBigDecimal previous = first = iterator.next();
        PointBigDecimal current = iterator.next();
        uiOld = new LinearFunctionBigDecimal(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                current.getValue().add(epsilon));
        liOld = new LinearFunctionBigDecimal(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                current.getValue().subtract(epsilon));

        while (true) {
            if (!iterator.hasNext()) {
                if (uiOld != null && liOld != null) {
//                  System.out.println("need to start new line");
                    LinearFunctionBigDecimal line = new LinearFunctionBigDecimal(first.getTimestamp(), first.getValue(),
                            current.getTimestamp(),
                            (uiOld.get(current.getTimestamp()).add(liOld.get(current.getTimestamp()))).divide(new BigDecimal(2), mc));
                    swingSegments.add(new SwingSegmentBigDecimal(first.getTimestamp(), current.getTimestamp(), line));
                } else {
                    swingSegments.add(new SwingSegmentBigDecimal(first.getTimestamp(), first.getTimestamp(), first.getValue(), first.getValue()));
                }
                return swingSegments;
            }
            previous = current;
            current = iterator.next();
//            System.out.println("Points: " + first.getValue() + "\t" + previous.getValue() + "\t" + current.getValue() + "\t" + uiOld + "\t" + liOld);
            if (uiOld.get(current.getTimestamp()).compareTo(current.getValue().subtract(epsilon)) < 0
                    || liOld.get(current.getTimestamp()).compareTo(current.getValue().add(epsilon)) > 0) {
                PointBigDecimal newPoint = new PointBigDecimal(previous.getTimestamp(), (uiOld.get(previous.getTimestamp()).add(liOld.get(previous.getTimestamp()))).divide(new BigDecimal(2), mc));
                System.out.println("need to start new line: " + first.getValue() + "\t" + newPoint.getValue() + "\t" + previous.getTimestamp());
                swingSegments.add(new SwingSegmentBigDecimal(first.getTimestamp(), previous.getTimestamp() - 1, first.getValue(), newPoint.getValue()));
//                swingSegments.add(new SwingSegment(first.getTimestamp(), previous.getTimestamp() - 1, first.getValue(), (uiOld.get(previous.getTimestamp()) + liOld.get(previous.getTimestamp())) / 2));
                previous = first = newPoint;
                uiOld = new LinearFunctionBigDecimal(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                        current.getValue().add(epsilon));
                liOld = new LinearFunctionBigDecimal(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                        current.getValue().subtract(epsilon));
//                System.out.println("New range: " + current.getValue() + "\t" + current.getTimestamp() + "\t" + uiOld + "\t" + liOld);
            } else {
                LinearFunctionBigDecimal uiNew = new LinearFunctionBigDecimal(first.getTimestamp(), first.getValue(),
                        current.getTimestamp(), current.getValue().add(epsilon));
                LinearFunctionBigDecimal liNew = new LinearFunctionBigDecimal(first.getTimestamp(), first.getValue(),
                        current.getTimestamp(), current.getValue().subtract(epsilon));
//                System.out.println("Cand: " + current.getValue() + "\t" + current.getTimestamp() + "\t" + uiNew + "\t" + liNew);
                if (uiOld == null || uiOld.get(current.getTimestamp()).compareTo(uiNew.get(current.getTimestamp())) > 0) {
                    uiOld = uiNew;
                }
                if (liOld == null || liOld.get(current.getTimestamp()).compareTo(liNew.get(current.getTimestamp())) < 0) {
                    liOld = liNew;
                }
            }
        }
    }
}
