package gr.aueb.delorean.chimp.benchmarks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class TimeseriesFileReader {
	public static final int DEFAULT_BLOCK_SIZE = 1_000;
	private static final String DELIMITER = ",";
	private static final int VALUE_POSITION = 2;
	BufferedReader bufferedReader;
	private int blocksize;

	public TimeseriesFileReader(InputStream inputStream) throws IOException {
		this(inputStream, DEFAULT_BLOCK_SIZE);
	}

	public TimeseriesFileReader(InputStream inputStream, int blocksize) throws IOException {
		InputStream gzipStream = new GZIPInputStream(inputStream);
		Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
		this.bufferedReader = new BufferedReader(decoder);
		this.blocksize = blocksize;
	}

	public double[] nextBlock() {
		double[] values = new double[DEFAULT_BLOCK_SIZE];
		String line;
		int counter = 0;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				try {
					double value = Double.parseDouble(line.split(DELIMITER)[VALUE_POSITION]);
					values[counter++] = value;
					if (counter == blocksize) {
						return values;
					}
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public float[] nextBlock32() {
		float[] values = new float[DEFAULT_BLOCK_SIZE];
		String line;
		int counter = 0;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				try {
					float value = Float.parseFloat(line.split(DELIMITER)[VALUE_POSITION]);
					values[counter++] = value;
					if (counter == blocksize) {
						return values;
					}
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public BigDecimal[] nextBlockBigDecimal() {
		BigDecimal[] values = new BigDecimal[DEFAULT_BLOCK_SIZE];
		String line;
		int counter = 0;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				try {
					BigDecimal value = new BigDecimal(line.split(DELIMITER)[VALUE_POSITION]);
					values[counter++] = value;
					if (counter == blocksize) {
						return values;
					}
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static double mean (List<Float> table)
	{
		double total = 0;

		for ( int i= 0;i < table.size(); i++)
		{
			float currentNum = table.get(i);
			total+= currentNum;
		}
		return total/table.size();
	}


	public static double sd (List<Float> list)
	{
		// Step 1:
		double mean = mean(list);
//		    System.out.println("Mean: " + mean + ", Media: " + list.get(list.size()/2));
		double temp = 0;

		for (int i = 0; i < list.size(); i++)
		{
			float val = list.get(i);

			// Step 2:
			double squrDiffToMean = Math.pow(val - mean, 2);

			// Step 3:
			temp += squrDiffToMean;
		}

		// Step 4:
		double meanOfDiffs = temp / (list.size());

		// Step 5:
		return Math.sqrt(meanOfDiffs);
	}


}
