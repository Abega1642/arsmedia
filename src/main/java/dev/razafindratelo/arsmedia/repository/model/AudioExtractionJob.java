package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "audio_extraction_job")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class AudioExtractionJob extends Job {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private JVideo parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "extracted_audio_id")
  private JAudio extractedAudio;

  public AudioExtractionJob(
      String id,
      JVideo parent,
      JAudio extractedAudio,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      ProcessStatus status,
      String errorMessage,
      int attemptCount) {
    super(id, createdAt, completedAt, status, errorMessage, attemptCount);
    this.parent = parent;
    this.extractedAudio = extractedAudio;
  }
}
