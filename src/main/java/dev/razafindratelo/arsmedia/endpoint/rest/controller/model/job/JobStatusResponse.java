package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
