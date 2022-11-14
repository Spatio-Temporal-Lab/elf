package gr.aueb.delorean.chimp.benchmarks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.io.compress.brotli.BrotliCodec;
import org.apache.hadoop.hbase.io.compress.lz4.Lz4Codec;
import org.apache.hadoop.hbase.io.compress.xerial.SnappyCodec;
import org.apache.hadoop.hbase.io.compress.xz.LzmaCodec;
import org.apache.hadoop.hbase.io.compress.zstd.ZstdCodec;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.junit.jupiter.api.Test;

import com.github.kutschkem.fpc.FpcCompressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import fi.iki.yak.ts.compression.gorilla.Compressor;
import fi.iki.yak.ts.compression.gorilla.Decompressor;
import gr.aueb.delorean.chimp.Chimp;
import gr.aueb.delorean.chimp.ChimpDecompressor;
import gr.aueb.delorean.chimp.ChimpN;
import gr.aueb.delorean.chimp.ChimpNDecompressor;
import gr.aueb.delorean.chimp.ChimpNNoIndex;

/**
 * These are generic tests to test that input matches the output after compression + decompression cycle, using
 * the value compression.
 *
 */
public class TestDoublePrecision {

    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
    private static String[] FILENAMES = {
            "/city_temperature.csv.gz",
            "/Stocks-Germany-sample.txt.gz",
            "/SSD_HDD_benchmarks.csv.gz"
    };

    @Test
    public void testChimp128() throws IOException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ChimpN compressor = new ChimpN(128);
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ChimpNDecompressor d = new ChimpNDecompressor(compressor.getOut(), 128);
                start = System.nanoTime();
                List<Double> uncompressedValues = d.getValues();
                decodingDuration += System.nanoTime() - start;
                for(int i=0; i<values.length; i++) {
                    assertEquals(values[i], uncompressedValues.get(i).doubleValue(), "Value did not match");
                }


            }
            System.out.println(String.format("Chimp128: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testChimp() throws IOException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                Chimp compressor = new Chimp();
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ChimpDecompressor d = new ChimpDecompressor(compressor.getOut());
                start = System.nanoTime();
                List<Double> uncompressedValues = d.getValues();
                decodingDuration += System.nanoTime() - start;
                for(int i=0; i<values.length; i++) {
                    assertEquals(values[i], uncompressedValues.get(i).doubleValue(), "Value did not match");
                }

            }
            System.out.println(String.format("Chimp: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testCorilla() throws IOException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBufferBitOutput output = new ByteBufferBitOutput();
                Compressor compressor = new Compressor(output);
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ByteBuffer byteBuffer = output.getByteBuffer();
                byteBuffer.flip();
                ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
                Decompressor d = new Decompressor(input);

                start = System.nanoTime();
                List<Double> uncompressedValues = d.getValues();
                decodingDuration += System.nanoTime() - start;
                for(int i=0; i<values.length; i++) {
                    assertEquals(values[i], uncompressedValues.get(i).doubleValue(), "Value did not match");
                }

            }
            System.out.println(String.format("Gorilla: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testFPC() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                FpcCompressor fpc = new FpcCompressor();

                ByteBuffer buffer = ByteBuffer.allocate(TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 10);
                // Compress
                long start = System.nanoTime();
                fpc.compress(buffer, values);
                encodingDuration += System.nanoTime() - start;

                totalSize += buffer.position() * 8;
                totalBlocks += 1;

                buffer.flip();

                FpcCompressor decompressor = new FpcCompressor();

                double[] dest = new double[TimeseriesFileReader.DEFAULT_BLOCK_SIZE];
                start = System.nanoTime();
                decompressor.decompress(buffer, dest);
                decodingDuration += System.nanoTime() - start;
                assertArrayEquals(dest, values);

            }
            System.out.println(String.format("FPC: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testChimp128NoIndex() throws IOException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ChimpNNoIndex compressor = new ChimpNNoIndex(128);
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ChimpNDecompressor d = new ChimpNDecompressor(compressor.getOut(), 128);
                start = System.nanoTime();
                List<Double> uncompressedValues = d.getValues();
                decodingDuration += System.nanoTime() - start;
                for(int i=0; i<values.length; i++) {
                    assertEquals(values[i], uncompressedValues.get(i).doubleValue(), "Value did not match");
                }


            }
            System.out.println(String.format("Chimp128-no-index: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }


    @Test
    public void testSnappy() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(double d : values) {
                    bb.putDouble(d);
                }
                byte[] input = bb.array();

                Configuration conf = HBaseConfiguration.create();
                // ZStandard levels range from 1 to 22.
                // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
                conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
                SnappyCodec codec = new SnappyCodec();
                codec.setConf(conf);

                // Compress
                long start = System.nanoTime();
                org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressionOutputStream out = codec.createOutputStream(baos, compressor);
                out.write(input);
                out.close();
                encodingDuration += System.nanoTime() - start;
                final byte[] compressed = baos.toByteArray();
                totalSize += compressed.length * 8;
                totalBlocks++;

                final byte[] plain = new byte[input.length];
                org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
                start = System.nanoTime();
                CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
                IOUtils.readFully(in, plain, 0, plain.length);
                in.close();
                double[] uncompressed = toDoubleArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Snappy: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }


    @Test
    public void testZstd() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(double d : values) {
                    bb.putDouble(d);
                }
                byte[] input = bb.array();

                Configuration conf = HBaseConfiguration.create();
                // ZStandard levels range from 1 to 22.
                // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
                conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
                ZstdCodec codec = new ZstdCodec();
                codec.setConf(conf);

                // Compress
                long start = System.nanoTime();
                org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressionOutputStream out = codec.createOutputStream(baos, compressor);
                out.write(input);
                out.close();
                encodingDuration += System.nanoTime() - start;
                final byte[] compressed = baos.toByteArray();
                totalSize += compressed.length * 8;
                totalBlocks++;

                final byte[] plain = new byte[input.length];
                org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
                start = System.nanoTime();
                CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
                IOUtils.readFully(in, plain, 0, plain.length);
                in.close();
                double[] uncompressed = toDoubleArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Zstd: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testLZ4() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(double d : values) {
                    bb.putDouble(d);
                }
                byte[] input = bb.array();

                Lz4Codec codec = new Lz4Codec();

                // Compress
                long start = System.nanoTime();
                org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressionOutputStream out = codec.createOutputStream(baos, compressor);
                out.write(input);
                out.close();
                encodingDuration += System.nanoTime() - start;
                final byte[] compressed = baos.toByteArray();
                totalSize += compressed.length * 8;
                totalBlocks++;

                final byte[] plain = new byte[input.length];
                org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
                start = System.nanoTime();
                CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
                IOUtils.readFully(in, plain, 0, plain.length);
                in.close();
                double[] uncompressed = toDoubleArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("LZ4: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testBrotli() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(double d : values) {
                    bb.putDouble(d);
                }
                byte[] input = bb.array();

                BrotliCodec codec = new BrotliCodec();

                // Compress
                long start = System.nanoTime();
                org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressionOutputStream out = codec.createOutputStream(baos, compressor);
                out.write(input);
                out.close();
                encodingDuration += System.nanoTime() - start;
                final byte[] compressed = baos.toByteArray();
                totalSize += compressed.length * 8;
                totalBlocks++;

                final byte[] plain = new byte[input.length];
                org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
                start = System.nanoTime();
                CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
                IOUtils.readFully(in, plain, 0, plain.length);
                in.close();
                double[] uncompressed = toDoubleArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Brotli: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    @Test
    public void testXz() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            double[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(double d : values) {
                    bb.putDouble(d);
                }
                byte[] input = bb.array();

                Configuration conf = new Configuration();
                // LZMA levels range from 1 to 9.
                // Level 9 might take several minutes to complete. 3 is our default. 1 will be fast.
                conf.setInt(LzmaCodec.LZMA_LEVEL_KEY, 3);
                LzmaCodec codec = new LzmaCodec();
                codec.setConf(conf);

                // Compress
                long start = System.nanoTime();
                org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressionOutputStream out = codec.createOutputStream(baos, compressor);
                out.write(input);
                out.close();
                encodingDuration += System.nanoTime() - start;
                final byte[] compressed = baos.toByteArray();
                totalSize += compressed.length * 8;
                totalBlocks++;

                final byte[] plain = new byte[input.length];
                org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
                start = System.nanoTime();
                CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
                IOUtils.readFully(in, plain, 0, plain.length);
                in.close();
                double[] uncompressed = toDoubleArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Xz: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }


    public static double[] toDoubleArray(byte[] byteArray){
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).getDouble();
        }
        return doubles;
    }

}
