package dev.razafindratelo.arsmedia.repository.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "auth_code",
    uniqueConstraints = @UniqueConstraint(columnNames = {"code", "created_at"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JAuthCode {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private JUser user;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false, name = "created_at")
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime deadline;
}
