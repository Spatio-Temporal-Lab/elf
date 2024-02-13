# Elf: Erasing-based Lossless Floating-Point Compression (VLDB 2023, Accepted)
# Erasing-Based Lossless Compression for Streaming Floating-Point Time Series (Termed Elf+)

***
Elf is an erasing-based floating-point data compression algorithm with a high compression ratio.

Developers can follow the steps below for compression testing.

More details can be found in http://start-db.urbcomp.org/elf/

(Please switch to the branch of ***vldb2023-release*** to test the performance of VLDB paper. For the ***dev*** branch, we have optimized the algorithm, which is under review of VLDBJ; thus, some logics of the ***dev*** branch may be not the same with those described in the VLDB paper. You are recommended to use the ***dev*** branch, because it should perform much better in terms both of compression ratio and compression time.)

## Elf feature

- Elf can greatly increase the number of trailing zeros in XORed results, which enhances the compression ratio with a
  theoretical guarantee
- Elf algorithm takes only O (1) in both time complexity and space complexity.
- Elf adopts an elaborated coding strategy for the XORed results with many trailing zeros.
- The erasing operation in this project can be used as a preprocessing step for all XOR-based compression algorithms.

## Project Structure

This project mainly includes the following various compression algorithms:

- The main code for the ***Elf*** algorithm is in the *org/urbcomp/startdb/compress/elf* package.

- The main code for the ***Chimp*** algorithm is in the *gr/aueb/delorean/chimp* package.

- The main code for the ***Gorilla*** algorithm is in the *fi/iki/yak/ts/compression/gorilla* package.

- The main code for the ***FPC*** algorithm is in the *com/github/kutschkem/fpc* package.

- The main code for other general compression algorithms is in the *org/apache/hadoop/hbase/io/compress* package.

### ELF Structure

ELF includes *compressor* and *decompressor* packages as well as *xorcompressor* and *xordecompressor*.

#### compressor package

This package includes 7 different XOR-based compression algorithms and provides a standard **ICompressor** interface. The
erasing operation is abstracted as **AbstractElfCompressor**.

- ElfCompressor: This class is the complete elf compression algorithm.
- ElfOnChimpCompressor: This class is pre-processed for erasure and then compressed using the Chimp algorithm.
- ElfOnChimpNCompressor: This class is pre-processed for erasure and then compressed using the Chimp128 algorithm.
- ElfOnGorillaCompressorOS: This class is pre-processed for erasure and then compressed using the Gorilla algorithm.
- GorillaCompressorOS: This class is the Gorilla algorithm using Bitstream I/O optimization.
- ChimpCompressor: This class is the original chimp algorithm.
- ChimpNCompressor: This class is the original chimp128 algorithm.

#### decompressor package

This package includes the decompressors corresponding to the above 7 compressors and gives the standard **IDecompressor** interface

#### xorcompressor package

This package is a compressed encoding of post-erase data designed for XOR-based operations

#### dexorcompressor package

This package is a decompression of the erased data designed based on the XOR-based operation code.

## TEST ELF

We recommend IntelliJ IDEA for developing this project. In our experiment, the default data block size is 1000. That is, 1000
pieces of data are read in each time for compression testing. If the size of the data set is less than 1000, we will not read it. The final experimental result is an average calculation of the compression of all data blocks.

### Prerequisites for testing

The following resources need to be downloaded and installed:

- Java 8 download: https://www.oracle.com/java/technologies/downloads/#java8
- IntelliJ IDEA download: https://www.jetbrains.com/idea/
- git download:https://git-scm.com/download
- maven download: https://archive.apache.org/dist/maven/maven-3/

Download and install jdk-8, IntelliJ IDEA and git. IntelliJ IDEA's maven project comes with maven, you can also use your
own maven environment, just change it in the settings.

### Clone code

1. Open *IntelliJ IDEA*, find the *git* column, and select *Clone...*

2. In the *Repository URL* interface, *Version control* selects *git*

3. URL filling: *https://github.com/Spatio-Temporal-Lab/elf.git*

### Set JDK

File -> Project Structure -> Project -> Project SDK -> *add SDK*

Click *JDK* to select the address where you want to download jdk-8

### Test ELF

Select the *org/urbcomp/startdb/compress/elf* package in the *test* folder, which includes tests for 64bits Double data
and 32bits Float data.

#### Double data test:

In *doubleprecision* package

- The **TestCompressor** class includes compression tests for 22 data sets. The test results are saved in *result/result.csv* in resource.
- The **TestBeta** class is a compression test for different beta of data. Two data sets with long mantissa are selected
  and different bits are reserved for compression test. The test results are saved in *result/resultBeta.csv* in
  resource.

#### Float data test:

In *singleprecision* package

- The **TestCompressor** class includes compression tests for 22 data sets. The test results are saved in *result32/result.csv* in resource.

### Use your own artifacts

In order to use Elf more conveniently, you must want to use your own data for testing, you can follow the steps below to
use your own data set for testing.

When you have a dataset of type double, for example in a csv file. If the form of the data is the same as the data set
given in the system, you can put your own dataset in the ***resources*** package, and then add the name of your own
dataset in ***FIleName*** in the ***TestCompressor*** class.

In our experiment, the default data block size is 1000. That is, 1000 pieces of data are read in each time for
compression testing, and the part of the data set smaller than 1000 cannot be read. If the data set is too small, the
result may be empty.

