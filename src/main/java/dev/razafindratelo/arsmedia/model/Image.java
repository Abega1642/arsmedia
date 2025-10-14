package dev.razafindratelo.arsmedia.model;

import dev.razafindratelo.arsmedia.model.classifier.ImageFormat;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Image extends Media {
  private int width;
  private int height;
  private ImageFormat format;
  private int bitDepth;
  private String colorModel;
  private double dpi;
  private int iso;
}
