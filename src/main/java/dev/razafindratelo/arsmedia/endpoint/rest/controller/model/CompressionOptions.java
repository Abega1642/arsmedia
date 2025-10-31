package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompressionOptions {
  private int crf = 23;
  private Integer targetWidth;
  private Integer targetHeight;
  private Long targetSizeMB;

  public static CompressionOptions defaults() {
    return new CompressionOptions(23, null, null, null);
  }
}
