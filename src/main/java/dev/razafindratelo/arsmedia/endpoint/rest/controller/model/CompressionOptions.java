package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompressionOptions {
  @JsonProperty("crf")
  private int crf = 23;

  @JsonProperty("target_width")
  private Integer targetWidth;

  @JsonProperty("target_height")
  private Integer targetHeight;

  @JsonProperty("target_size_mb")
  private Long targetSizeMB;

  public static CompressionOptions defaults() {
    return new CompressionOptions(23, null, null, null);
  }
}
