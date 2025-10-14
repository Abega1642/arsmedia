package dev.razafindratelo.arsmedia.repository.model;

import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "video")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JVideo extends JMedia {

  private double duration;
  private String codec;
  private int width;
  private int height;

  @Column(name = "frame_rate")
  private double frameRate;

  @Column(name = "aspect_ratio")
  private String aspectRatio;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "container_format")
  private ContainerFormat containerFormat;

  @Column(name = "bit_rate")
  private double bitRate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "audio_codec")
  private AudioCodec audioCodec;

  @Column(name = "audio_channels")
  private int audioChannels;

  @Column(name = "audio_simple_rate")
  private int audioSampleRate;
}
