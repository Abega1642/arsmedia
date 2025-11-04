package dev.razafindratelo.arsmedia.repository.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_client_secret")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class JApiClientSecret {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "api_client_id", nullable = false)
  private JApiClient apiClient;

  @Column(nullable = false)
  private String secret;
}
