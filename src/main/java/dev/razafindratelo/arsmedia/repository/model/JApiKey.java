package dev.razafindratelo.arsmedia.repository.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "api_key")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JApiKey {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private JUser owner;

  @Column(name = "api_key", nullable = false)
  private String apiKey;

  @Column(nullable = false)
  private LocalDateTime creation;

  @Column(nullable = false)
  private LocalDateTime expiration;
}
