package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode
@ToString
public class JobStatusResponse {
  private String jobId;
  private ProcessStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private String errorMessage;
  private int attemptCount;
}
