package gr.aueb.delorean.chimp.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import gr.aueb.delorean.chimp.Chimp32;
import gr.aueb.delorean.chimp.ChimpDecompressor32;
import gr.aueb.delorean.chimp.ChimpN32;
import gr.aueb.delorean.chimp.ChimpNDecompressor32;
import gr.aueb.delorean.chimp.Compressor32;
import gr.aueb.delorean.chimp.Decompressor32;
import gr.aueb.delorean.chimp.Value;



/**
 * These are generic tests to test that input matches the output after compression + decompression cycle, using
 * the value compression.
 *
 */
public class TestSinglePrecision {

	private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
	private static String[] FILENAMES = {
	        "/city_temperature.csv.gz",
	        "/Stocks-Germany-sample.txt.gz",
	        "/SSD_HDD_benchmarks.csv.gz"
			};



	@Test
    public void testChimpN32() throws IOException {
        int previousValues = 64;

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
                ChimpN32 compressor = new ChimpN32(previousValues);
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue((float) value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ChimpNDecompressor32 d = new ChimpNDecompressor32(compressor.getOut(), previousValues);
                for(Double value : values) {
                    start = System.nanoTime();
                    Float pair = d.readValue();
                    decodingDuration += System.nanoTime() - start;
                  assertEquals(value.floatValue(), pair.floatValue(), "Value did not match");
                }
              assertNull(d.readValue());

            }
            System.out.println(String.format("Chimp32-64: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }


	@Test
    public void testChimp32() throws IOException {
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
                Chimp32 compressor = new Chimp32();
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue((float) value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ChimpDecompressor32 d = new ChimpDecompressor32(compressor.getOut());
                for(Double value : values) {
                    start = System.nanoTime();
                    Float pair = d.readValue();
                    decodingDuration += System.nanoTime() - start;
                    assertEquals(value.floatValue(), pair.floatValue(), "Value did not match");
                }
                assertNull(d.readValue());

            }
            System.out.println(String.format("Chimp32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testCorilla32() throws IOException {
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
                Compressor32 compressor = new Compressor32(output);
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue((float) value);
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;

                ByteBuffer byteBuffer = output.getByteBuffer();
                byteBuffer.flip();
                ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
                Decompressor32 d = new Decompressor32(input);
                for(Double value : values) {
                    start = System.nanoTime();
                    Value pair = d.readValue();
                    decodingDuration += System.nanoTime() - start;
                    assertEquals(value.floatValue(), pair.getFloatValue(), "Value did not match");
                }
                assertNull(d.readValue());

            }
            System.out.println(String.format("Gorilla32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testSnappy32() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            float[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock32()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock32();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(float d : values) {
                   bb.putFloat(d);
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
                float[] uncompressed = toFloatArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Snappy32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testZstd32() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            float[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock32()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock32();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(float d : values) {
                   bb.putFloat(d);
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
                float[] uncompressed = toFloatArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Zstd32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testLZ432() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            float[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock32()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock32();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(float d : values) {
                   bb.putFloat(d);
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
                float[] uncompressed = toFloatArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("LZ4-32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testBrotli32() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            float[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock32()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock32();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 4);
                for(float d : values) {
                   bb.putFloat(d);
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
                float[] uncompressed = toFloatArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Brotli32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

	@Test
    public void testXz32() throws IOException, InterruptedException {
        for (String filename : FILENAMES) {
            TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
            long totalSize = 0;
            float totalBlocks = 0;
            float[] values;
            long encodingDuration = 0;
            long decodingDuration = 0;
            while ((values = timeseriesFileReader.nextBlock32()) != null || totalBlocks < MINIMUM_TOTAL_BLOCKS) {
                if (values == null) {
                    timeseriesFileReader = new TimeseriesFileReader(getClass().getResourceAsStream(filename));
                    values = timeseriesFileReader.nextBlock32();
                }
                ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
                for(float d : values) {
                   bb.putFloat(d);
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
                float[] uncompressed = toFloatArray(plain);
                decodingDuration += System.nanoTime() - start;
                // Decompressed bytes should equal the original
                for(int i = 0; i < values.length; i++) {
                    assertEquals(values[i], uncompressed[i], "Value did not match");
                }
            }
            System.out.println(String.format("Xz32: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
        }
    }

    public static float[] toFloatArray(byte[] byteArray){
        int times = Float.SIZE / Byte.SIZE;
        float[] floats = new float[byteArray.length / times];
        for(int i=0;i<floats.length;i++){
            floats[i] = ByteBuffer.wrap(byteArray, i*times, times).getFloat();
        }
        return floats;
    }


}
