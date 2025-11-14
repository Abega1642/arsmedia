package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "video_compression_job")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoCompressionJob extends Job {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private JVideo parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "compressed_video_id")
  private JVideo compressedVideo;

  public VideoCompressionJob(
      String id,
      JVideo parent,
      JVideo compressedVideo,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      ProcessStatus status,
      String errorMessage,
      int attemptCount) {
    super(id, createdAt, completedAt, status, errorMessage, attemptCount);
    this.parent = parent;
    this.compressedVideo = compressedVideo;
  }
}
