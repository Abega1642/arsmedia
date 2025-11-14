package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job")
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Job {
  @Id private String id;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "process_status", nullable = false)
  private ProcessStatus status;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount = 0;
}
