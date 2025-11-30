package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BitRateCalculatorTest {

  private final BitRateCalculator subject = new BitRateCalculator();

  @TempDir Path tempDir;

  @Test
  void should_calculate_bitrate_for_valid_file() throws IOException {
    var file = givenFileWithSize(1_000_000); // 1 MB
    double duration = 10.0; // 10 seconds

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 800.0); // 800 kbps
  }

  @Test
  void should_calculate_bitrate_as_integer() throws IOException {
    var file = givenFileWithSize(1_250_000); // 1.25 MB
    double duration = 10.0; // 10 seconds

    int bitrate = whenCalculateAsInt(file, duration);

    thenBitrateIsInt(bitrate, 1000); // 1000 kbps
  }

  @Test
  void should_calculate_bitrate_for_small_file() throws IOException {
    var file = givenFileWithSize(125_000); // 125 KB
    double duration = 5.0; // 5 seconds

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 200.0); // 200 kbps
  }

  @Test
  void should_calculate_bitrate_for_large_file() throws IOException {
    var file = givenFileWithSize(100_000_000); // 100 MB
    double duration = 60.0; // 1 minute

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 13333.333333333334);
  }

  @Test
  void should_calculate_bitrate_with_fractional_duration() throws IOException {
    var file = givenFileWithSize(500_000); // 500 KB
    double duration = 2.5; // 2.5 seconds

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 1600.0); // 1600 kbps
  }

  @Test
  void should_calculate_bitrate_for_very_short_duration() throws IOException {
    var file = givenFileWithSize(10_000); // 10 KB
    double duration = 0.1; // 0.1 seconds

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 800.0); // 800 kbps
  }

  @Test
  void should_calculate_bitrate_for_very_long_duration() throws IOException {
    var file = givenFileWithSize(360_000_000); // 360 MB
    double duration = 3600.0; // 1 hour

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 800.0); // 800 kbps
  }

  @Test
  void should_return_zero_for_null_file() {
    double bitrate = whenCalculate(null, 10.0);

    thenBitrateIsZero(bitrate);
  }

  @Test
  void should_return_zero_for_non_existent_file() {
    var nonExistentFile = givenNonExistentFile();

    double bitrate = whenCalculate(nonExistentFile, 10.0);

    thenBitrateIsZero(bitrate);
  }

  @Test
  void should_return_zero_for_zero_duration() throws IOException {
    var file = givenFileWithSize(1_000_000);

    double bitrate = whenCalculate(file, 0.0);

    thenBitrateIsZero(bitrate);
  }

  @Test
  void should_return_zero_for_negative_duration() throws IOException {
    var file = givenFileWithSize(1_000_000);

    double bitrate = whenCalculate(file, -5.0);

    thenBitrateIsZero(bitrate);
  }

  @Test
  void should_return_zero_as_integer_for_invalid_input() throws IOException {
    var file = givenFileWithSize(1_000_000);

    int bitrate = whenCalculateAsInt(file, 0.0);

    thenBitrateIsZeroInt(bitrate);
  }

  @Test
  void should_handle_empty_file() throws IOException {
    var file = givenFileWithSize(0); // 0 bytes

    double bitrate = whenCalculate(file, 10.0);

    thenBitrateIsZero(bitrate);
  }

  @Test
  void should_calculate_bitrate_for_typical_video() throws IOException {
    var file = givenFileWithSize(52_428_800); // 50 MB
    double duration = 300.0; // 5 minutes

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 1398.1013333333333); // Typical HD video bitrate
  }

  @Test
  void should_calculate_bitrate_for_typical_audio() throws IOException {
    var file = givenFileWithSize(3_750_000); // ~3.75 MB
    double duration = 180.0; // 3 minutes

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 166.66666666666666); // Typical MP3 bitrate ~160 kbps
  }

  @Test
  void should_truncate_decimal_when_calculating_as_integer() throws IOException {
    var file = givenFileWithSize(999_999); // a non-round bitrate
    double duration = 7.3;

    int bitrate = whenCalculateAsInt(file, duration);

    thenBitrateIsInt(bitrate, 1_095);
  }

  @Test
  void should_handle_very_small_bitrate() throws IOException {
    var file = givenFileWithSize(100); // 100 bytes
    double duration = 1000.0; // 1000 seconds

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 0.0008); // 0.0008 kbps
  }

  @Test
  void should_handle_precision_with_double_calculation() throws IOException {
    var file = givenFileWithSize(1_234_567);
    double duration = 12.345;

    double bitrate = whenCalculate(file, duration);

    thenBitrateIs(bitrate, 800.0434183880113);
  }

  @Test
  void should_return_same_bitrate_for_multiple_calculations() throws IOException {
    var file = givenFileWithSize(5_000_000);
    double duration = 25.0;

    double bitrate1 = whenCalculate(file, duration);
    double bitrate2 = whenCalculate(file, duration);
    double bitrate3 = whenCalculate(file, duration);

    thenAllBitratesAreEqual(bitrate1, bitrate2, bitrate3);
  }

  @Test
  void should_calculate_different_bitrates_for_different_files() throws IOException {
    var file1 = givenFileWithSize(1_000_000);
    var file2 = givenFileWithSize(2_000_000);
    double duration = 10.0;

    double bitrate1 = whenCalculate(file1, duration);
    double bitrate2 = whenCalculate(file2, duration);

    thenBitrateIs(bitrate1, 800.0);
    thenBitrateIs(bitrate2, 1_600.0);
  }

  @Test
  void should_calculate_different_bitrates_for_different_durations() throws IOException {
    var file = givenFileWithSize(1_000_000);

    double bitrate1 = whenCalculate(file, 10.0);
    double bitrate2 = whenCalculate(file, 20.0);

    thenBitrateIs(bitrate1, 800.0);
    thenBitrateIs(bitrate2, 400.0);
  }

  private File givenFileWithSize(long sizeInBytes) throws IOException {
    var file = tempDir.resolve("test_" + System.nanoTime() + ".dat").toFile();

    try (var fos = new FileOutputStream(file)) {
      byte[] data = new byte[8_192];
      long remaining = sizeInBytes;

      while (remaining > 0) {
        int toWrite = (int) Math.min(data.length, remaining);
        fos.write(data, 0, toWrite);
        remaining -= toWrite;
      }
    }

    assertEquals(sizeInBytes, file.length(), "File size should match");
    return file;
  }

  private File givenNonExistentFile() {
    return new File(tempDir.toFile(), "nonexistent.mp4");
  }

  private double whenCalculate(File file, double duration) {
    return subject.calculate(file, duration);
  }

  private int whenCalculateAsInt(File file, double duration) {
    return subject.calculateAsInt(file, duration);
  }

  private void thenBitrateIs(double actual, double expected) {
    assertEquals(expected, actual, 0.000001, "Bitrate should be " + expected + " kbps");
  }

  private void thenBitrateIsInt(int actual, int expected) {
    assertEquals(expected, actual, "Bitrate should be " + expected + " kbps");
  }

  private void thenBitrateIsZero(double bitrate) {
    assertEquals(0.0, bitrate, 0.000001, "Bitrate should be 0");
  }

  private void thenBitrateIsZeroInt(int bitrate) {
    assertEquals(0, bitrate, "Bitrate should be 0");
  }

  private void thenAllBitratesAreEqual(double... bitrates) {
    for (int i = 1; i < bitrates.length; i++) {
      assertEquals(bitrates[0], bitrates[i], 0.000001, "All bitrates should be equal");
    }
  }
}
