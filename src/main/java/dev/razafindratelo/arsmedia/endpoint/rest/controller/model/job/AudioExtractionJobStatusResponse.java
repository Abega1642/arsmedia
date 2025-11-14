package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AudioExtractionJobStatusResponse extends JobStatusResponse {
  private String extractedAudioId;
  private String extractedAudioBucketKey;

  public AudioExtractionJobStatusResponse(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String extractedAudioId,
      String extractedAudioBucketKey,
      String errorMessage,
      int attemptCount) {
    super(jobId, status, createdAt, completedAt, errorMessage, attemptCount);
    this.extractedAudioId = extractedAudioId;
    this.extractedAudioBucketKey = extractedAudioBucketKey;
  }
}
