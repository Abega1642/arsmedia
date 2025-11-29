package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VideoFormatConversionJobStatusResponse extends JobStatusResponse {
  private String convertedVideoId;
  private String convertedVideoBucketKey;

  public VideoFormatConversionJobStatusResponse(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String convertedVideoId,
      String convertedVideoBucketKey,
      String errorMessage,
      int attemptCount) {
    super(jobId, status, createdAt, completedAt, errorMessage, attemptCount);
    this.convertedVideoId = convertedVideoId;
    this.convertedVideoBucketKey = convertedVideoBucketKey;
  }
}
