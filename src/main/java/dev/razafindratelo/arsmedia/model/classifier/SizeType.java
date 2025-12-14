package dev.razafindratelo.arsmedia.model.classifier;

public enum SizeType {
  BYTES,
  KB,
  MB,
  GB,
  TB,
  PB,
  EB;

  private static final long SIZE_KB = 1_024L;
  private static final long SIZE_MB = SIZE_KB * SIZE_KB;
  private static final long SIZE_GB = SIZE_MB * SIZE_KB;
  private static final long SIZE_TB = SIZE_GB * SIZE_KB;
  private static final long SIZE_PB = SIZE_TB * SIZE_KB;
  private static final long SIZE_EB = SIZE_PB * SIZE_KB;

  public static double convert(long bytes, SizeType targetUnit) {
    return switch (targetUnit) {
      case BYTES -> bytes;
      case KB -> bytes / (double) SIZE_KB;
      case MB -> bytes / (double) SIZE_MB;
      case GB -> bytes / (double) SIZE_GB;
      case TB -> bytes / (double) SIZE_TB;
      case PB -> bytes / (double) SIZE_PB;
      case EB -> bytes / (double) SIZE_EB;
    };
  }

  public static SizeType bestFit(long bytes) {
    if (bytes < SIZE_KB) return BYTES;
    if (bytes < SIZE_MB) return KB;
    if (bytes < SIZE_GB) return MB;
    if (bytes < SIZE_TB) return GB;
    if (bytes < SIZE_PB) return TB;
    if (bytes < SIZE_EB) return PB;
    return EB;
  }
}
