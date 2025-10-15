package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
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
}
