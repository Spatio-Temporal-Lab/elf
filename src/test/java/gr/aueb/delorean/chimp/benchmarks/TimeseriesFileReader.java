package gr.aueb.delorean.chimp.benchmarks;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	public static boolean gzipCompression(String filePath, String resultFilePath) throws IOException {
		System.out.println(" gzipCompression -> Compression start!");
		InputStream fin = null;
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		BufferedOutputStream bos= null;
		GZIPOutputStream gcos = null;
		try {
			fin = Files.newInputStream(Paths.get(filePath));
			bis = new BufferedInputStream(fin);
			fos = new FileOutputStream(resultFilePath);
			bos = new BufferedOutputStream(fos);
			gcos = new GZIPOutputStream(bos);
			byte[] buffer = new byte[1024];
			int read = -1;
			while ((read = bis.read(buffer)) != -1) {
				gcos.write(buffer, 0, read);
			}
		} finally {
			if(gcos != null)
				gcos.close();
			if(bos != null)
				bos.close();
			if(fos != null)
				fos.close();
			if(bis != null)
				bis.close();
			if(fin != null)
				fin.close();
		}
		System.out.println(" gzipCompression -> Compression end!");
		return true;
	}

//	public static void main(String[] args) throws IOException {
//		gzipCompression("D:\\workplace\\github\\Java\\start-compress\\src\\test\\resources\\taxi_data.csv","D:\\workplace\\github\\Java\\start-compress\\src\\test\\resources\\taxi_data.csv.gz");
//	}
}
