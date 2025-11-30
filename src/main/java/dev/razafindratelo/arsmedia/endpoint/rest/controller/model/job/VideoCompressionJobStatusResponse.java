package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VideoCompressionJobStatusResponse extends JobStatusResponse {
  private String compressedVideoId;
  private String compressedVideoUrl;
}
