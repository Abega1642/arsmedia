package dev.razafindratelo.arsmedia.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import java.time.LocalDateTime;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class User {
  private final String id;
  private final String email;
  private String pseudo;
  private String phoneNumber;
  private String imageProfileBucketKey;
  private UserRole role;
  private String password;
  private boolean isActivated;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
