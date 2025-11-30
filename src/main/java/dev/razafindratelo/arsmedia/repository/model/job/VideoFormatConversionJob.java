package dev.razafindratelo.arsmedia.repository.model.job;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "video_format_conversion_job")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoFormatConversionJob extends Job {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private JVideo parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "converted_video_id")
  private JVideo convertedVideo;

  public VideoFormatConversionJob(
      String id,
      JVideo parent,
      JVideo convertedVideo,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      ProcessStatus status,
      String errorMessage,
      int attemptCount) {
    super(id, createdAt, completedAt, status, errorMessage, attemptCount);
    this.parent = parent;
    this.convertedVideo = convertedVideo;
  }
}
