package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.model.Image;
import dev.razafindratelo.arsmedia.repository.model.JImage;

public class ImageMapper {

  private ImageMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Image toImage(JImage j) {
    var img =
        new Image(
            j.getWidth(),
            j.getHeight(),
            j.getFormat(),
            j.getBitDepth(),
            j.getColorModel(),
            j.getDpi(),
            j.getIso());

    img.setId(j.getId());
    img.setFileName(j.getFileName());
    img.setSize(j.getSize());
    img.setSizeType(j.getSizeType());
    img.setFileType(j.getFileType());
    img.setCreatedAt(j.getCreatedAt());
    img.setFilePath(j.getBucketKey());

    return img;
  }

  public static JImage toJImage(Image i) {
    JImage j = new JImage();
    j.setId(i.getId());
    j.setFileName(i.getFileName());
    j.setSize(i.getSize());
    j.setSizeType(i.getSizeType());
    j.setFileType(i.getFileType());
    j.setCreatedAt(i.getCreatedAt());
    j.setWidth(i.getWidth());
    j.setHeight(i.getHeight());
    j.setFormat(i.getFormat());
    j.setBitDepth(i.getBitDepth());
    j.setColorModel(i.getColorModel());
    j.setDpi(i.getDpi());
    j.setIso(i.getIso());
    return j;
  }
}
