package dev.razafindratelo.arsmedia.model.classifier;

public enum SizeType {
  BYTES,
  KB,
  MB,
  GB,
  TB,
  PB,
  EB;

  public static double convert(long bytes, SizeType targetUnit) {
    return switch (targetUnit) {
      case BYTES -> bytes;
      case KB -> bytes / 1_024.0;
      case MB -> bytes / 1_048_576.0;
      case GB -> bytes / 1_073_741_824.0;
      case TB -> bytes / 1_099_511_627_776.0;
      case PB -> bytes / 1_125_899_906_842_624.0;
      case EB -> bytes / 1_152_921_504_606_846_976.0;
    };
  }

  public static SizeType bestFit(long bytes) {
    if (bytes < 1_024L) return BYTES;
    if (bytes < 1_048_576L) return KB;
    if (bytes < 1_073_741_824L) return MB;
    if (bytes < 1_099_511_627_776L) return GB;
    if (bytes < 1_125_899_906_842_624L) return TB;
    if (bytes < 1_152_921_504_606_846_976L) return PB;
    return EB;
  }
}
