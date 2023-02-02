# Elf: Erasing-based Lossless Floating-Point Compression

***
elf is an erasure-based floating-point data compression algorithm with a high compression ratio.

Developers can follow the steps below for compression testing.

## Elf feature

***
- Elf can greatly increase the number of trailing zeros in XORed results,
which enhances the compression ratio with a theoretical guarantee
- Elf algorithm takes only
  O (1) in both time complexity and space complexity.
- ELf adopt unique coding method for the XORed results with many trailing zeros.

## TEST ELF

***
We recommend IntelliJ IDEA for developing projects.

### Prerequisites for testing

The following resources need to be downloaded and installed:

- Java 8 download: https://www.oracle.com/java/technologies/downloads/#java8
- IntelliJ IDEA download: https://www.jetbrains.com/idea/
- git download:https://git-scm.com/download

Download and install jdk-8, IntelliJ IDEA and git.

### Clone code

1. Open *IntelliJ IDEA*, find the *git* column, and select *Clone...*

2. In the *Repository URL* interface, *Version control* selects *git*

3. URL filling: *https://github.com/Spatio-Temporal-Lab/elf.git*

### Set JDK

File -> Project Structure -> Project -> Project SDK -> *add SDK*

Click *JDK* to select the address where you want to download jdk-8

### Test ELF

Select the *org/urbcomp/startdb/compress/elf* package in the *test* folder, which includes tests for 64bits Double data
and 32bits Float data

####Double data test:
In *doubleprecision* package
- The **TestCompressor** class includes compression tests for 22 data sets. The test results are saved in *result/result.csv* in
  resource.
- The **TestBeta** class is a compression test for different beta of data. Two data sets with long mantissa are selected and
  different bits are reserved for compression test. The test results are saved in *result/resultBeta.csv* in resource.

####Float data test:
In *singleprecision* package
- The **TestCompressor** class includes compression tests for 22 data sets. The test results are saved in *result32/result.csv* in
  resource.
***