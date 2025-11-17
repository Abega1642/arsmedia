package dev.razafindratelo.arsmedia.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "api_client")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class JApiClient {
  @Id private String id;

  @Column(name = "client_name", nullable = false)
  private String clientName;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false, name = "phone_number")
  private String phoneNumber;
}
