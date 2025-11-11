package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.UserMapper.toJUser;
import static dev.razafindratelo.arsmedia.mapper.UserMapper.toUser;

import dev.razafindratelo.arsmedia.model.Media;
import dev.razafindratelo.arsmedia.repository.model.JMedia;

public class MediaMapper {

  private MediaMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Media toMedia(JMedia jMedia) {
    return new Media(
        jMedia.getId(),
        jMedia.getFileName(),
        jMedia.getSize(),
        jMedia.getSizeType(),
        jMedia.getFileType(),
        jMedia.getCreatedAt(),
        jMedia.getBucketKey(),
        toUser(jMedia.getOwner()));
  }

  public static JMedia toJMedia(Media media) {
    return new JMedia(
        media.getId(),
        media.getFileName(),
        media.getSize(),
        media.getSizeType(),
        media.getFileType(),
        media.getCreatedAt(),
        media.getFilePath(),
        toJUser(media.getOwner()));
  }
}
