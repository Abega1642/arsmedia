package dev.razafindratelo.arsmedia.repository.model.token;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "token")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JToken {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private JUser user;

  @Column(name = "is_valid")
  private boolean isValid;

  private String value;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private TokenType type;

  private LocalDateTime creation;
  private LocalDateTime expiration;
}
