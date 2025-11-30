package dev.razafindratelo.arsmedia.model.classifier;

public enum VideoCodec {
  H264,
  H265,
  MPEG1,
  MPEG2,
  MPEG4,
  VP8,
  VP9,
  AV1,
  THEORA,
  WMV1,
  WMV2,
  WMV3,
  MJPEG,
  PRORES,
  DNXHD,
  CINEFORM,
  DV,
  XVID,
  DIVX,
  HEVC,
  FLV,
  H263,
  UNKNOWN;

  public static VideoCodec fromString(String value) {
    if (value == null) return UNKNOWN;
    String v = value.trim().toUpperCase();
    return switch (v) {
      case "H.264", "H264", "AVC" -> H264;
      case "H.265", "H265", "HEVC" -> H265;
      case "MPEG1VIDEO", "MPEG1" -> MPEG1;
      case "MPEG2VIDEO", "MPEG2" -> MPEG2;
      case "MPEG4" -> MPEG4;
      case "VP8" -> VP8;
      case "VP9" -> VP9;
      case "AV1" -> AV1;
      case "THEORA" -> THEORA;
      case "WMV1" -> WMV1;
      case "WMV2" -> WMV2;
      case "WMV3" -> WMV3;
      case "MJPEG" -> MJPEG;
      case "PRORES" -> PRORES;
      case "DNXHD" -> DNXHD;
      case "CINEFORM" -> CINEFORM;
      case "DV", "DVVIDEO" -> DV;
      case "XVID" -> XVID;
      case "DIVX" -> DIVX;
      case "FLV", "FLV1" -> FLV;
      case "H.263", "H263" -> H263;
      default -> UNKNOWN;
    };
  }
}
