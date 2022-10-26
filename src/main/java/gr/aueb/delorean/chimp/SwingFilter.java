package gr.aueb.delorean.chimp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SwingFilter {

    public List<SwingSegment> filter(Collection<Point> points, float epsilon) {

        List<SwingSegment> swingSegments = new ArrayList<>();

        Point first = null;
        LinearFunction uiOld = null;
        LinearFunction liOld = null;

        Iterator<Point> iterator = points.iterator();

        Point previous = first = iterator.next();
        Point current = iterator.next();
        uiOld = new LinearFunction(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                current.getValue() + epsilon);
        liOld = new LinearFunction(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                current.getValue() - epsilon);

        while (true) {
            if (!iterator.hasNext()) {
                if (uiOld != null && liOld != null) {
                    LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(),
                            current.getTimestamp(),
                            (uiOld.get(current.getTimestamp()) + liOld.get(current.getTimestamp())) / 2);
                    swingSegments.add(new SwingSegment(first.getTimestamp(), current.getTimestamp(), line));
                } else {
                	LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(),
                            first.getTimestamp() + 1, first.getValue());
                    swingSegments.add(new SwingSegment(first.getTimestamp(), first.getTimestamp(), line));
                }
                return swingSegments;
            }
            previous = current;
            current = iterator.next();
            if (uiOld.get(current.getTimestamp()) < current.getValue() - epsilon
                    || liOld.get(current.getTimestamp()) > current.getValue() + epsilon) {

            	LinearFunction line = new LinearFunction(first.getTimestamp(), first.getValue(),
                        previous.getTimestamp(), (uiOld.get(previous.getTimestamp()) + liOld.get(previous.getTimestamp())) / 2);
                swingSegments.add(new SwingSegment(first.getTimestamp(), previous.getTimestamp() - 1, line));
                previous = first = new Point(previous.getTimestamp(), (float) line.get(previous.getTimestamp()));
                uiOld = new LinearFunction(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                        current.getValue() + epsilon);
                liOld = new LinearFunction(previous.getTimestamp(), previous.getValue(), current.getTimestamp(),
                        current.getValue() - epsilon);
            } else {
                LinearFunction uiNew = new LinearFunction(first.getTimestamp(), first.getValue(),
                        current.getTimestamp(), current.getValue() + epsilon);
                LinearFunction liNew = new LinearFunction(first.getTimestamp(), first.getValue(),
                        current.getTimestamp(), current.getValue() - epsilon);
                if (uiOld == null || uiOld.get(current.getTimestamp()) > uiNew.get(current.getTimestamp())) {
                    uiOld = uiNew;
                }
                if (liOld == null || liOld.get(current.getTimestamp()) < liNew.get(current.getTimestamp())) {
                    liOld = liNew;
                }
            }
        }
    }
}
