package dev.razafindratelo.arsmedia.model.classifier;

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

  public static AudioCodec fromString(String value) {
    if (value == null) return UNKNOWN;
    String v = value.trim().toUpperCase();
    return switch (v) {
      case "AAC" -> AAC;
      case "MP3", "MPEG AUDIO LAYER 3" -> MP3;
      case "MP2" -> MP2;
      case "AC3" -> AC3;
      case "EAC3" -> EAC3;
      case "DTS" -> DTS;
      case "PCM", "PCM_S16LE" -> PCM_S16LE;
      case "PCM_S24LE" -> PCM_S24LE;
      case "FLAC" -> FLAC;
      case "OPUS" -> OPUS;
      case "VORBIS" -> VORBIS;
      case "WMA" -> WMA;
      case "ALAC" -> ALAC;
      case "AMR", "AMR_NB" -> AMR_NB;
      case "AMR_WB" -> AMR_WB;
      case "G711" -> G711;
      case "G722" -> G722;
      default -> UNKNOWN;
    };
  }
}
