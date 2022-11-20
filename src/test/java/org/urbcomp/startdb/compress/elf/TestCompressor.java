package org.urbcomp.startdb.compress.elf;

import com.github.kutschkem.fpc.FpcCompressor;
import gr.aueb.delorean.chimp.benchmarks.TimeseriesFileReader;
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
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources";
    private static final String[] FILENAMES = {
            "/AvgTemperature.csv",
//            "/circuits_lat.csv",
//            "/circuits_lng.csv",
//            "/diskCapability.csv",
//            "/location-lat.csv",
//            "/location-long.csv",
//            "/mp_price.csv",
//            "/pitStop_duration.csv",
//            "/worldcities_latitude.csv",
//            "/worldcities_longitude.csv",
//            "/x-axis.csv",
//            "/y-axis.csv",
//            "/z-axis.csv",
//            "/NewYork_temperature.csv",
//            "/Tokyo_temperature.csv",
//            "/l4d2_player_stats.csv",
//            "/percentage_of_alcohol.csv",
//            "/electric_vehicle_charging.csv",
//        "/ECMWF Interim Full Daily Invariant High Vegetation Cover.csv",
//        "/ECMWF Interim Full Daily Invariant Low Vegetation Cover.csv"
    };
    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
    private static final String STORE_PATH = "src/test/resources/result";

    private static double TIME_PRECISION = 1000.0;
    List<Map<String, List<ResultStructure>>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        for (String filename : FILENAMES) {
            Map<String, List<ResultStructure>> result = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                testELFCompressor(filename, result);
                testFPC(filename,result);
                testSnappy(filename,result);
                testZstd(filename,result);
                testLZ4(filename,result);
                testBrotli(filename,result);
                testXz(filename,result);
            }
            allResult.add(result);
        }
        storeResult(STORE_PATH + "/result.dat");
    }


    public void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        ICompressor[] compressorList = new ICompressor[]{
                //new GorillaCompressorOS(),
                new ElfOnGorillaCompressorOS(),
                //new ChimpCompressor(),
                //new ElfOnChimpCompressor(),
                new ChimpNCompressor(128),
                new ElfOnChimpNCompressor(128),
                new ElfCompressor(128),
                new ElfCompressor(64),
                new ElfCompressor(32),
                new ElfCompressor(16),
                new ElfCompressor(8),
                new ElfCompressor(4),
                new ElfCompressor(2),
        };
        float totalBlocks = 0;
        long[] totalSize = new long[compressorList.length];
        long[] encodingDuration = new long[compressorList.length];
        long[] decodingDuration = new long[compressorList.length];
        double[] values;
        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
            ICompressor[] compressors = new ICompressor[]{
                    //new GorillaCompressorOS(),
                    new ElfOnGorillaCompressorOS(),
                    //new ChimpCompressor(),
                    //new ElfOnChimpCompressor(),
                    new ChimpNCompressor(128),
                    new ElfOnChimpNCompressor(128),
                    new ElfCompressor(128),
                    new ElfCompressor(64),
                    new ElfCompressor(32),
                    new ElfCompressor(16),
                    new ElfCompressor(8),
                    new ElfCompressor(4),
                    new ElfCompressor(2),
            };
            for (int i = 0; i < compressors.length; i++) {
                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration[i] += System.nanoTime() - start;

                totalSize[i] += compressor.getSize();

                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
                        //new GorillaDecompressorOS(result),
                        new ElfOnGorillaDecompressorOS(result),
                        //new ChimpDecompressor(result),
                        //new ElfOnChimpDecompressor(result),
                        new ChimpNDecompressor(result, 128),
                        new ElfOnChimpNDecompressor(result, 128),
                        new ElfDecompressor(result, 128),
                        new ElfDecompressor(result, 64),
                        new ElfDecompressor(result, 32),
                        new ElfDecompressor(result, 16),
                        new ElfDecompressor(result, 8),
                        new ElfDecompressor(result, 4),
                        new ElfDecompressor(result, 2)
                };

                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration[i] += System.nanoTime() - start;

                for (int j = 0; j < values.length; j++) {
                    assertEquals(values[j], uncompressedValues.get(j), "Value did not match");
                }
            }
        }
        for (int i = 0; i < compressorList.length; i++) {
            String key = compressorList[i].getKey();
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize[i] / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    encodingDuration[i] / totalBlocks,
                    decodingDuration[i] / totalBlocks
            );
            if (!resultCompressor.containsKey(compressorList[i].getKey())) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testFPC(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "FPC";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }

    public void testSnappy(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "Snappy";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }

    public void testZstd(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "Zstd";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }

    public void testLZ4(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "LZ4";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }

    public void testBrotli(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "Brotli";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }

    public void testXz(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        long encodingDuration = 0;
        long decodingDuration = 0;
        while ((values = fileReader.nextBlock()) != null) {
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
        String key = "Xz";
        ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                encodingDuration / totalBlocks,
                decodingDuration / totalBlocks
        );
        if (!resultCompressor.containsKey(key)) {
            resultCompressor.put(key, new ArrayList<>());
        }
        resultCompressor.get(key).add(r);
    }



    public double[] computeDrawParameter(List<ResultStructure> result) {
        List<Double> compressTimeList = new ArrayList<>();
        List<Double> decompressTimeList = new ArrayList<>();
        for (ResultStructure r : result) {
            compressTimeList.add(r.getCompressionTime());
            decompressTimeList.add(r.getDecompressionTime());
        }
        double[] drawParam = new double[7];
        drawParam[0] = result.get(0).getCompressorRatio();
        drawParam[1] = medianValue(compressTimeList);
        drawParam[2] = quarterLowValue(compressTimeList);
        drawParam[3] = quarterHighValue(compressTimeList);
        drawParam[4] = medianValue(decompressTimeList);
        drawParam[5] = quarterLowValue(decompressTimeList);
        drawParam[6] = quarterHighValue(decompressTimeList);
        return drawParam;
    }

    public double medianValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        if (num % 2 == 1) {
            return ld.get(num / 2);
        } else {
            return (ld.get(num / 2) + ld.get(num / 2 - 1)) / 2;
        }
    }

    public double quarterLowValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num / 4);
    }

    public double quarterHighValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num * 3 / 4);
    }

    public void storeResult(String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        for (Map<String, List<ResultStructure>> result : allResult) {
            for (List<ResultStructure> ls : result.values()) {
                double[] param = computeDrawParameter(ls);
                fileWriter.write(ls.get(0).getFilename() + "\t");
                fileWriter.write(ls.get(0).getCompressorName());
                for (double p : param) {
                    fileWriter.write("\t" + p);
                }
                fileWriter.write("\n");
            }
        }
        fileWriter.close();
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
