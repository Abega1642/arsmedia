package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.repository.model.JVideo;

public class VideoMapper {

  private VideoMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static Video toVideo(JVideo jVideo) {
    Video video =
        new Video(
            jVideo.getDuration(),
            jVideo.getCodec(),
            jVideo.getWidth(),
            jVideo.getHeight(),
            jVideo.getFrameRate(),
            jVideo.getAspectRatio(),
            jVideo.getContainerFormat(),
            jVideo.getBitRate(),
            jVideo.getAudioCodec(),
            jVideo.getAudioChannels(),
            jVideo.getAudioSampleRate());

    video.setId(jVideo.getId());
    video.setFileName(jVideo.getFileName());
    video.setFilePath(jVideo.getBucketKey());
    video.setSize(jVideo.getSize());
    video.setSizeType(jVideo.getSizeType());
    video.setFileType(jVideo.getFileType());
    video.setCreatedAt(jVideo.getCreatedAt());
    video.setOwner(UserMapper.toUser(jVideo.getOwner()));

    return video;
  }

  public static JVideo toJVideo(Video video) {
    JVideo jVideo = new JVideo();
    jVideo.setId(video.getId());
    jVideo.setFileName(video.getFileName());
    jVideo.setOwner(UserMapper.toJUser(video.getOwner()));
    jVideo.setSize(video.getSize());
    jVideo.setSizeType(video.getSizeType());
    jVideo.setBucketKey(video.getFilePath());
    jVideo.setFileType(video.getFileType());
    jVideo.setCreatedAt(video.getCreatedAt());
    jVideo.setDuration(video.getDuration());
    jVideo.setCodec(video.getCodec());
    jVideo.setWidth(video.getWidth());
    jVideo.setHeight(video.getHeight());
    jVideo.setFrameRate(video.getFrameRate());
    jVideo.setAspectRatio(video.getAspectRatio());
    jVideo.setContainerFormat(video.getContainerFormat());
    jVideo.setBitRate(video.getBitRate());
    jVideo.setAudioCodec(video.getAudioCodec());
    jVideo.setAudioChannels(video.getAudioChannels());
    jVideo.setAudioSampleRate(video.getAudioSampleRate());
    return jVideo;
  }
}
