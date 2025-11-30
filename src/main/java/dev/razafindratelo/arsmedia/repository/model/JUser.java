package dev.razafindratelo.arsmedia.repository.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class JUser {
  @Id private String id;

  @Column(unique = true, nullable = false)
  private String email;

  private String pseudo;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "img_profile_bucket_key")
  private String imageProfileBucketKey;

  private String password;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(NAMED_ENUM)
  @Column(name = "user_role")
  private UserRole role;

  @Column(name = "is_activated")
  private boolean isActivated;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
