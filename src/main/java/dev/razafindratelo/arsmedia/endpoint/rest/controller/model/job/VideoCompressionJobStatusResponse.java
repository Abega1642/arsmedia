package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VideoCompressionJobStatusResponse extends JobStatusResponse {
  private String compressedVideoId;
  private String compressedVideoUrl;

  public VideoCompressionJobStatusResponse(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String compressedVideoId,
      String compressedVideoUrl,
      String errorMessage,
      int attemptCount) {
    super(jobId, status, createdAt, completedAt, errorMessage, attemptCount);
    this.compressedVideoId = compressedVideoId;
    this.compressedVideoUrl = compressedVideoUrl;
  }
}
