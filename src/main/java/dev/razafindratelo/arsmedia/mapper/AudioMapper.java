package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.repository.model.JAudio;

public class AudioMapper {

  private AudioMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Audio toAudio(JAudio jAudio) {
    Audio audio =
        new Audio(
            jAudio.getDuration(),
            jAudio.getBitRate(),
            jAudio.getSampleRate(),
            jAudio.getChannels(),
            jAudio.getCodec(),
            jAudio.getFormat());

    audio.setFileName(jAudio.getFileName());
    audio.setId(jAudio.getId());
    audio.setSize(jAudio.getSize());
    audio.setSizeType(jAudio.getSizeType());
    audio.setFileType(jAudio.getFileType());
    audio.setCreatedAt(jAudio.getCreatedAt());
    audio.setFilePath(jAudio.getBucketKey());
    return audio;
  }

  public static JAudio toJAudio(Audio audio) {
    JAudio jAudio =
        new JAudio(
            audio.getDuration(),
            audio.getBitRate(),
            audio.getSampleRate(),
            audio.getChannels(),
            audio.getCodec(),
            audio.getFormat());

    jAudio.setId(audio.getId());
    jAudio.setFileName(audio.getFileName());
    jAudio.setSize(audio.getSize());
    jAudio.setSizeType(audio.getSizeType());
    jAudio.setFileType(audio.getFileType());
    jAudio.setCreatedAt(audio.getCreatedAt());
    jAudio.setBucketKey(audio.getFilePath());
    return jAudio;
  }
}
