package dev.razafindratelo.arsmedia.model.classifier;

public enum ImageFormat {
  JPEG,
  PNG,
  GIF,
  BMP,
  TIFF,
  WEBP,
  HEIF,
  HEIC,
  RAW,
  SVG,
  ICO,
  PSD,
  EXR,
  DNG,
  CR2,
  ARW,
  NEF,
  RAF,
  UNKNOWN;

  public static ImageFormat fromString(String value) {
    if (value == null) return UNKNOWN;
    String v = value.trim().toUpperCase();
    return switch (v) {
      case "JPEG", "JPG" -> JPEG;
      case "PNG" -> PNG;
      case "GIF" -> GIF;
      case "BMP" -> BMP;
      case "TIFF", "TIF" -> TIFF;
      case "WEBP" -> WEBP;
      case "HEIF" -> HEIF;
      case "HEIC" -> HEIC;
      case "RAW" -> RAW;
      case "SVG" -> SVG;
      case "ICO" -> ICO;
      case "PSD" -> PSD;
      case "EXR" -> EXR;
      case "DNG" -> DNG;
      case "CR2" -> CR2;
      case "ARW" -> ARW;
      case "NEF" -> NEF;
      case "RAF" -> RAF;
      default -> UNKNOWN;
    };
  }
}
