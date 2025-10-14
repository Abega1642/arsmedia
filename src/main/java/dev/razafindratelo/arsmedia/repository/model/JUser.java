package dev.razafindratelo.arsmedia.repository.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JUser {
  @Id private String id;
  private String email;
  private String pseudo;
  private String phoneNumber;
  private String imageProfileBucketKey;
  private String password;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(NAMED_ENUM)
  @Column(name = "user_role")
  private UserRole role;

  private boolean isActivated;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
