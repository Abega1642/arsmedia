package dev.razafindratelo.arsmedia.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class User implements UserDetails {
  private String id;
  private String email;
  private String pseudo;
  private String phoneNumber;
  private String imageProfileBucketKey;
  private UserRole role;
  private String password;
  private boolean isActivated;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(
        new SimpleGrantedAuthority(String.format("ROLE_%s", role.toString())));
  }

  @Override
  public String getUsername() {
    return pseudo;
  }
}
