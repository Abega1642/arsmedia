package dev.razafindratelo.arsmedia.repository.mapper;

import dev.razafindratelo.arsmedia.model.Media;
import dev.razafindratelo.arsmedia.repository.model.JMedia;

public class MediaMapper {
  public static Media toMedia(JMedia jMedia) {
    return new Media(
        jMedia.getId(),
        jMedia.getFileName(),
        jMedia.getSize(),
        jMedia.getSizeType(),
        jMedia.getFileType(),
        jMedia.getCreatedAt(),
        jMedia.getBucketKey());
  }

  public static JMedia toJMedia(Media media) {
    return new JMedia(
        media.getId(),
        media.getFileName(),
        media.getSize(),
        media.getSizeType(),
        media.getFileType(),
        media.getCreatedAt(),
        media.getFilePath());
  }
}
