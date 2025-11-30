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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AudioExtractionJobStatusResponse extends JobStatusResponse {
  private String extractedAudioId;
  private String extractedAudioBucketKey;
}
