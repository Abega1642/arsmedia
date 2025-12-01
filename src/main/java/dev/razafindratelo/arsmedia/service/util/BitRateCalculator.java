package dev.razafindratelo.arsmedia.service.util;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BitRateCalculator {
  private static final double ZERO_DURATION = 0.0;
  private static final double ZERO_BITRATE = 0.0;
  private static final int BITS_PER_BYTE = 8;
  private static final int KILOBITS_DIVISOR = 1_000;

  public double calculate(File file, double durationInSeconds) {
    if (!isValidInput(file, durationInSeconds)) {
      log.debug(
          "Invalid input for bitrate calculation: file={}, duration={}",
          file != null ? file.getName() : "null",
          durationInSeconds);
      return ZERO_BITRATE;
    }

    double bitrate = computeBitrate(file, durationInSeconds);

    log.debug(
        "Calculated bitrate: {} kbps for file: {} (size: {} bytes, duration: {} seconds)",
        String.format("%.2f", bitrate),
        file.getName(),
        file.length(),
        durationInSeconds);

    return bitrate;
  }

  public int calculateAsInt(File file, double durationInSeconds) {
    return (int) calculate(file, durationInSeconds);
  }

  private boolean isValidInput(File file, double durationInSeconds) {
    if (file == null) {
      log.warn("Cannot calculate bitrate: file is null");
      return false;
    }

    if (!file.exists()) {
      log.warn("Cannot calculate bitrate: file does not exist: {}", file.getAbsolutePath());
      return false;
    }

    if (durationInSeconds <= ZERO_DURATION) {
      log.debug("Cannot calculate bitrate: invalid duration: {}", durationInSeconds);
      return false;
    }

    return true;
  }

  private double computeBitrate(File file, double durationInSeconds) {
    long fileSizeInBytes = file.length();
    long fileSizeInBits = fileSizeInBytes * BITS_PER_BYTE;
    return fileSizeInBits / durationInSeconds / KILOBITS_DIVISOR;
  }
}
