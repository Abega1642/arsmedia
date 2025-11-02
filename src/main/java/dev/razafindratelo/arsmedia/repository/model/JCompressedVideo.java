package dev.razafindratelo.arsmedia.repository.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "compressed_videos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JCompressedVideo {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private JVideo parent;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
