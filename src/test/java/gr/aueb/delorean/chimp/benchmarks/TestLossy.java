package gr.aueb.delorean.chimp.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import gr.aueb.delorean.chimp.DecompressorPmcMr;
import gr.aueb.delorean.chimp.DecompressorSwingFilter;
import gr.aueb.delorean.chimp.PmcMR;
import gr.aueb.delorean.chimp.PmcMR.Constant;
import gr.aueb.delorean.chimp.Point;
import gr.aueb.delorean.chimp.SwingFilter;
import gr.aueb.delorean.chimp.SwingSegment;

/**
 * These are generic tests to test that input matches the output after compression + decompression cycle, using
 * the value compression.
 *
 */
public class TestLossy {

	private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
	private static String[] FILENAMES = {
	        "/city_temperature.csv.gz",
	        "/Stocks-Germany-sample.txt.gz",
	        "/SSD_HDD_benchmarks.csv.gz"
			};

	@Test
    public void testPmcMr() throws IOException {

        for (String filename : FILENAMES) {
            for (int logOfError = -7; logOfError < 12; logOfError++) {
                float epsilon = ((float) Math.pow(2, logOfError));
                TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                double[] values;
                double maxValue = Double.MIN_VALUE;
                double minValue = Double.MAX_VALUE;
                int timestamp = 0;
                double maxPrecisionError = 0;
                long totalSize = 0;
                float totalBlocks = 0;
                double totalStdev = 0D;
                long encodingDuration = 0;
                long decodingDuration = 0;
                while ((values = timeseriesFileReader.nextBlock()) != null && totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                    Collection<Point> points = new ArrayList<>();
                    for (Double value : values) {
                        points.add(new Point(timestamp++, value.floatValue()));
                    }

                    long start = System.nanoTime();
                    List<Constant> constants = new PmcMR().filter(points, epsilon);
                    encodingDuration += System.nanoTime() - start;

                    totalStdev += TimeseriesFileReader.sd(points.stream().map(l -> (float) l.getValue()).collect(Collectors.toList()));
                    totalBlocks += 1;
                    totalSize += constants.size() * 2 * 32;

                    DecompressorPmcMr d = new DecompressorPmcMr(constants);
                    for (Double value : values) {
                        start = System.nanoTime();
                        maxValue = value > maxValue ? value : maxValue;
                        minValue = value < minValue ? value : minValue;
                        Float decompressedValue = d.readValue();
                        decodingDuration += System.nanoTime() - start;
                        double precisionError = Math.abs(value.doubleValue() - decompressedValue);
                        maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
                        assertEquals(value.floatValue(), decompressedValue.floatValue(), epsilon, "Value did not match");
                    }
                }
                System.out.println(String.format(
                        "PMC-MR: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f, Error: %.8f, STDEV: %.2f, Error/STDEV: %.2f, Range: %.2f (%.2f)",
                        filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE),
                        encodingDuration / totalBlocks, decodingDuration / totalBlocks, maxPrecisionError, totalStdev / totalBlocks, maxPrecisionError / (totalStdev / totalBlocks), (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue)));

            }
        }
    }

	@Test
    public void testSwing() throws IOException {

        for (String filename : FILENAMES) {
            for (int logOfError = -7; logOfError < 12; logOfError++) {
                float epsilon = ((float) Math.pow(2, logOfError));
                TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                double[] values;
                double maxValue = Double.MIN_VALUE;
                double minValue = Double.MAX_VALUE;
                int timestamp = 0;
                double maxPrecisionError = 0;
                long totalSize = 32;
                float totalBlocks = 0;
                double totalStdev = 0D;
                long encodingDuration = 0;
                long decodingDuration = 0;
                while ((values = timeseriesFileReader.nextBlock()) != null && totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                    Collection<Point> points = new ArrayList<>();
                    for (Double value : values) {
                        points.add(new Point(timestamp++, value.floatValue()));
                    }

                    long start = System.nanoTime();
                    List<SwingSegment> constants = new SwingFilter().filter(points, epsilon);
                    encodingDuration += System.nanoTime() - start;

                    totalStdev += TimeseriesFileReader.sd(points.stream().map(l -> (float) l.getValue()).collect(Collectors.toList()));
                    totalBlocks += 1;
                    totalSize += constants.size() * (2 * 32);

                    DecompressorSwingFilter d = new DecompressorSwingFilter(constants);
                    for (Double value : values) {
                        start = System.nanoTime();
                        maxValue = value > maxValue ? value : maxValue;
                        minValue = value < minValue ? value : minValue;
                        Float decompressedValue = d.readValue();
                        decodingDuration += System.nanoTime() - start;
                        double precisionError = Math.abs(value.doubleValue() - decompressedValue);
                        maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
                        assertEquals(value.floatValue(), decompressedValue.floatValue(), epsilon + epsilon / 10000, "Value did not match");
                    }
                }
                System.out.println(String.format(
                        "Swing: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f, Error: %.8f, STDEV: %.2f, Error/STDEV: %.2f, Range: %.2f (%.2f)",
                        filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE),
                        encodingDuration / totalBlocks, decodingDuration / totalBlocks, maxPrecisionError, totalStdev / totalBlocks, maxPrecisionError / (totalStdev / totalBlocks), (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue)));

            }
        }
    }


	/*
	@Test
    public void testSwingBigDecimal() throws IOException {
        BigDecimal[] epsilons = {new BigDecimal("0.25"), new BigDecimal("0.5")};
        for (String filename : FILENAMES) {
            for (BigDecimal epsilon : epsilons) {
                TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                BigDecimal[] values;
                BigDecimal maxValue = new BigDecimal(Double.MIN_VALUE);
                BigDecimal minValue = new BigDecimal(Double.MAX_VALUE);
                int timestamp = 0;
                double maxPrecisionError = 0;
                long totalSize = 32;
                float totalBlocks = 0;
                double totalStdev = 0D;
                long encodingDuration = 0;
                long decodingDuration = 0;
                while ((values = timeseriesFileReader.nextBlockBigDecimal()) != null && totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                    Collection<PointBigDecimal> points = new ArrayList<>();
                    for (BigDecimal value : values) {
                        points.add(new PointBigDecimal(timestamp++, value));
                    }

                    long start = System.nanoTime();
                    List<SwingSegmentBigDecimal> constants = new SwingFilterBigDecimal().filter(points, epsilon);
                    encodingDuration += System.nanoTime() - start;

                    totalStdev += TimeseriesFileReader.sd(points.stream().map(l -> l.getValue().floatValue()).collect(Collectors.toList()));
                    totalBlocks += 1;
                    totalSize += constants.size() * (2 * 32);

                    DecompressorSwingFilterBigDecimal d = new DecompressorSwingFilterBigDecimal(constants);
                    for (BigDecimal value : values) {
                        start = System.nanoTime();
                        maxValue = value.compareTo(maxValue) > 0 ? value : maxValue;
                        minValue = value.compareTo(minValue) < 0 ? value : minValue;
                        BigDecimal decompressedValue = d.readValue();
                        decodingDuration += System.nanoTime() - start;
                        double precisionError = Math.abs(value.doubleValue() - decompressedValue.doubleValue());
                        maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
                        System.out.println(value.floatValue() + "\t" + decompressedValue.floatValue() + "\t" + epsilon + "\t" + (value.floatValue()+ epsilon.floatValue() >= decompressedValue.floatValue()));
                        assertTrue(value.add(epsilon).compareTo(decompressedValue) > 0, "Value did not match");
                        assertTrue(value.subtract(epsilon).compareTo(decompressedValue) < 0, "Value did not match");
                    }
                }
                System.out.println(String.format(
                        "Swing: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f, Error: %.8f, STDEV: %.2f, Error/STDEV: %.2f, Range: %.2f (%.2f)",
                        filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE),
                        encodingDuration / totalBlocks, decodingDuration / totalBlocks, maxPrecisionError, totalStdev / totalBlocks, maxPrecisionError / (totalStdev / totalBlocks), (maxValue.subtract(minValue)), 100* maxPrecisionError / (maxValue.subtract(minValue).doubleValue())));

            }
        }
    }

    @Test
    public void testSwingD() throws IOException {

        for (String filename : FILENAMES) {
            for (int logOfError = -7; logOfError < 12; logOfError++) {
                TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                double[] values;
                double maxValue = Double.MIN_VALUE;
                double minValue = Double.MAX_VALUE;
                int timestamp = 0;
                double maxPrecisionError = 0;
                long totalSize = 32;
                float totalBlocks = 0;
                double totalStdev = 0D;
                long encodingDuration = 0;
                long decodingDuration = 0;
                while ((values = timeseriesFileReader.nextBlock()) != null && totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                    Collection<Point> points = new ArrayList<>();
                    for (Double value : values) {
                        points.add(new Point(timestamp++, value.floatValue()));
                    }

                    long start = System.nanoTime();
                    List<SwingSegment> constants = new SwingFilterDisjoint().filter(points, (float) (Math.pow(2, logOfError)));
                    encodingDuration += System.nanoTime() - start;

                    totalStdev += TimeseriesFileReader.sd(points.stream().map(l -> (float) l.getValue()).collect(Collectors.toList()));
                    totalBlocks += 1;
                    totalSize += constants.size() * ( 2 * 64 + 32);

                    DecompressorSwingFilter d = new DecompressorSwingFilter(constants);
                    for (Double value : values) {
                        start = System.nanoTime();
                        maxValue = value > maxValue ? value : maxValue;
                        minValue = value < minValue ? value : minValue;
                        Float decompressedValue = d.readValue();
                        decodingDuration += System.nanoTime() - start;
                        double precisionError = Math.abs(value.doubleValue() - decompressedValue);
                        maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
//                        System.out.println(value.floatValue() + "\t" + decompressedValue.floatValue() + "\t" + Math.pow(2, logOfError) + "\t" + (value.floatValue()+Math.pow(2, logOfError) >= decompressedValue.floatValue()));
                        assertEquals(value.floatValue(), decompressedValue.floatValue(), (float) Math.pow(2, logOfError), "Value did not match");
                    }
                }
                System.out.println(String.format(
                        "SwingD: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f, Error: %.8f, STDEV: %.2f, Error/STDEV: %.2f, Range: %.2f (%.2f)",
                        filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE),
                        encodingDuration / totalBlocks, decodingDuration / totalBlocks, maxPrecisionError, totalStdev / totalBlocks, maxPrecisionError / (totalStdev / totalBlocks), (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue)));

            }
        }
    }

    @Test
    public void testSwingDQ() throws IOException {

        for (String filename : FILENAMES) {
            for (int logOfError = -7; logOfError < 12; logOfError++) {
            	float epsilon = (float) Math.pow(2, logOfError);
                TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                double[] values;
                double maxValue = Double.MIN_VALUE;
                double minValue = Double.MAX_VALUE;
                int timestamp = 0;
                double maxPrecisionError = 0;
                long totalSize = 32;
                float totalBlocks = 0;
                double totalStdev = 0D;
                long encodingDuration = 0;
                long decodingDuration = 0;
                while ((values = timeseriesFileReader.nextBlock()) != null && totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                    List<Point> points = new ArrayList<>();
                    for (Double value : values) {
                        points.add(new Point(timestamp++, value.floatValue()));
                    }

                    long start = System.nanoTime();
                    List<SwingSegment> constants = new SwingFilterDisjointQuantized().filter(points, epsilon);
                    encodingDuration += System.nanoTime() - start;

                    totalStdev += TimeseriesFileReader.sd(points.stream().map(l -> (float) l.getValue()).collect(Collectors.toList()));
                    totalBlocks += 1;
                    totalSize += constants.size() * ( 2 * 64 + 32);

                    DecompressorSwingFilter d = new DecompressorSwingFilter(constants);
                    for (Double value : values) {
                        start = System.nanoTime();
                        maxValue = value > maxValue ? value : maxValue;
                        minValue = value < minValue ? value : minValue;
                        Float decompressedValue = d.readValue();
                        decodingDuration += System.nanoTime() - start;
                        double precisionError = Math.abs(value.doubleValue() - decompressedValue);
                        maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
                        assertEquals(value.floatValue(), decompressedValue.floatValue(), epsilon + epsilon / 1000, "Value did not match");
                    }
                }
                System.out.println(String.format(
                        "SwingDQ: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f, Error: %.8f, STDEV: %.2f, Error/STDEV: %.2f, Range: %.2f (%.2f)",
                        filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE),
                        encodingDuration / totalBlocks, decodingDuration / totalBlocks, maxPrecisionError, totalStdev / totalBlocks, maxPrecisionError / (totalStdev / totalBlocks), (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue)));

            }
        }
    }
*/

}
