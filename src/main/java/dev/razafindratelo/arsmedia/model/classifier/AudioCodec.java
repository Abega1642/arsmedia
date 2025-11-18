package dev.razafindratelo.arsmedia.model.classifier;

import java.util.Map;

public enum AudioCodec {
  NONE,
  AAC,
  MP3,
  MP2,
  AC3,
  EAC3,
  DTS,
  PCM,
  PCM_S16LE,
  PCM_S24LE,
  FLAC,
  OPUS,
  VORBIS,
  WMA,
  ALAC,
  AMR_NB,
  AMR_WB,
  G711,
  G722,
  UNKNOWN;

  private static final Map<String, AudioCodec> CODEC_MAP =
      Map.ofEntries(
          Map.entry("AAC", AAC),
          Map.entry("MP3", MP3),
          Map.entry("MPEG AUDIO LAYER 3", MP3),
          Map.entry("MP2", MP2),
          Map.entry("AC3", AC3),
          Map.entry("EAC3", EAC3),
          Map.entry("DTS", DTS),
          Map.entry("PCM", PCM_S16LE),
          Map.entry("PCM_S16LE", PCM_S16LE),
          Map.entry("PCM_S24LE", PCM_S24LE),
          Map.entry("FLAC", FLAC),
          Map.entry("OPUS", OPUS),
          Map.entry("VORBIS", VORBIS),
          Map.entry("WMA", WMA),
          Map.entry("ALAC", ALAC),
          Map.entry("AMR", AMR_NB),
          Map.entry("AMR_NB", AMR_NB),
          Map.entry("AMR_WB", AMR_WB),
          Map.entry("G711", G711),
          Map.entry("G722", G722));

  public static AudioCodec fromString(String value) {
    if (value == null) {
      return UNKNOWN;
    }
    return CODEC_MAP.getOrDefault(value.trim().toUpperCase(), UNKNOWN);
  }
}
