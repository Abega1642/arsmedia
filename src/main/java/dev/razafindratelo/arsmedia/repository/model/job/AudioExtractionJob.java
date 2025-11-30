package dev.razafindratelo.arsmedia.repository.model.job;

import dev.razafindratelo.arsmedia.repository.model.JAudio;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "audio_extraction_job")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString
public class AudioExtractionJob extends Job {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private JVideo parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "extracted_audio_id")
  private JAudio extractedAudio;
}
