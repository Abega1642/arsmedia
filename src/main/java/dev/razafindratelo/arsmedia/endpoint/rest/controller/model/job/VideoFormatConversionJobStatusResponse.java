package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VideoFormatConversionJobStatusResponse extends JobStatusResponse {
  private String convertedVideoId;
  private String convertedVideoBucketKey;
}
