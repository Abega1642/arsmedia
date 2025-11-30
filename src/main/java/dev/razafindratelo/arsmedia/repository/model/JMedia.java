package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "media")
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
public class JMedia {

  @Id private String id;

  @Column(nullable = false, name = "file_name")
  private String fileName;

  @Column(nullable = false)
  private double size;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, name = "size_type")
  private SizeType sizeType;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, name = "file_type")
  private FileType fileType;

  @Column(nullable = false, name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "bucket_key", nullable = false)
  private String bucketKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private JUser owner;
}
