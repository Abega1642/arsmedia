package dev.razafindratelo.arsmedia.model.classifier;

import java.util.Map;

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

  private static final Map<String, ContainerFormat> FORMAT_MAP =
      Map.ofEntries(
          Map.entry("MP4", MP4),
          Map.entry("MPEG-4", MP4),
          Map.entry("MKV", MKV),
          Map.entry("MATROSKA", MKV),
          Map.entry("MOV", MOV),
          Map.entry("QUICKTIME", MOV),
          Map.entry("AVI", AVI),
          Map.entry("FLV", FLV),
          Map.entry("WMV", WMV),
          Map.entry("ASF", WMV),
          Map.entry("WEBM", WEBM),
          Map.entry("MPEGTS", MPEG_TS),
          Map.entry("TS", MPEG_TS),
          Map.entry("MPEG-TS", MPEG_TS),
          Map.entry("MPEGPS", MPEG_PS),
          Map.entry("PS", MPEG_PS),
          Map.entry("MPEG-PS", MPEG_PS),
          Map.entry("MP3", MP3),
          Map.entry("MPEG", MP3),
          Map.entry("3GP", THREEGP),
          Map.entry("THREEGP", THREEGP),
          Map.entry("OGG", OGG),
          Map.entry("M4A", M4A),
          Map.entry("WAV", WAV),
          Map.entry("FLAC", FLAC),
          Map.entry("AIFF", AIFF),
          Map.entry("APE", APE));

  public static ContainerFormat fromString(String value) {
    if (value == null) {
      return UNKNOWN;
    }

    for (String token : value.trim().split(",")) {
      String normalized = token.trim().toUpperCase();
      ContainerFormat format = FORMAT_MAP.get(normalized);
      if (format != null) {
        return format;
      }
    }

    return UNKNOWN;
  }
}
