package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompressionJobStatusResponse {
  private String jobId;
  private ProcessStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private String compressedVideoId;
  private String compressedVideoUrl;
  private String errorMessage;
  private int attemptCount;
}
