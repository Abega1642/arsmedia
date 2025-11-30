package dev.razafindratelo.arsmedia.model;

import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Video extends Media {
  private double duration;
  private VideoCodec codec;
  private int width;
  private int height;
  private double frameRate;
  private String aspectRatio;
  private ContainerFormat containerFormat;
  private double bitRate;
  private AudioCodec audioCodec;
  private int audioChannels;
  private int audioSampleRate;
}
