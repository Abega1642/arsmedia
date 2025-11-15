package dev.razafindratelo.arsmedia.model.classifier;

public enum ContainerFormat {
  MP4,
  MP3,
  MKV,
  MOV,
  AVI,
  FLV,
  WMV,
  WEBM,
  MPEG_TS,
  MPEG_PS,
  THREEGP,
  OGG,
  M4A,
  WAV,
  FLAC,
  ASF,
  APE,
  AIFF,
  UNKNOWN;

  public static ContainerFormat fromString(String value) {
    if (value == null) return UNKNOWN;
    for (String token : value.trim().split(",")) {
      String v = token.trim().toUpperCase();
      ContainerFormat format =
          switch (v) {
            case "MP4", "MPEG-4" -> MP4;
            case "MKV", "MATROSKA" -> MKV;
            case "MOV", "QUICKTIME" -> MOV;
            case "AVI" -> AVI;
            case "FLV" -> FLV;
            case "WMV", "ASF" -> WMV;
            case "WEBM" -> WEBM;
            case "MPEGTS", "TS", "MPEG-TS" -> MPEG_TS;
            case "MPEGPS", "PS", "MPEG-PS" -> MPEG_PS;
            case "MP3", "MPEG" -> MP3;
            case "3GP", "THREEGP" -> THREEGP;
            case "OGG" -> OGG;
            case "M4A" -> M4A;
            case "WAV" -> WAV;
            case "FLAC" -> FLAC;
            case "AIFF" -> AIFF;
            case "APE" -> APE;
            default -> UNKNOWN;
          };
      if (format != UNKNOWN) return format;
    }
    return UNKNOWN;
  }
}
