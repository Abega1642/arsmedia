package dev.razafindratelo.arsmedia.service.media;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VideoMetaDataExtractor implements MediaMetadataExtractor<Video> {
  private final FFprobe ffprobe;

  public VideoMetaDataExtractor() throws IOException {
    try {
      this.ffprobe = new FFprobe("/usr/bin/ffprobe");
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Video apply(File file) {
    try {
      FFmpegProbeResult probeResult = ffprobe.probe(file.getAbsolutePath());

      FFmpegStream videoStream =
          probeResult.getStreams().stream()
              .filter(s -> s.codec_type == FFmpegStream.CodecType.VIDEO)
              .findFirst()
              .orElse(null);

      FFmpegStream audioStream =
          probeResult.getStreams().stream()
              .filter(s -> s.codec_type == FFmpegStream.CodecType.AUDIO)
              .findFirst()
              .orElse(null);

      Video video = new Video();
      video.setId(UUID.randomUUID().toString());
      video.setFileName(file.getName());
      video.setFileType(FileType.VIDEO);
      video.setSize(file.length());
      video.setSizeType(SizeType.BYTES);
      video.setCreatedAt(LocalDateTime.now());

      if (videoStream != null) {
        video.setCodec(VideoCodec.fromString(videoStream.codec_name));
        video.setWidth(videoStream.width);
        video.setHeight(videoStream.height);
        video.setFrameRate(
            (videoStream.avg_frame_rate != null && videoStream.avg_frame_rate.getDenominator() != 0)
                ? videoStream.avg_frame_rate.getNumerator()
                    / (double) videoStream.avg_frame_rate.getDenominator()
                : 0.0);
        video.setAspectRatio(videoStream.display_aspect_ratio);
        video.setBitRate(videoStream.bit_rate);
        video.setContainerFormat(ContainerFormat.fromString(probeResult.getFormat().format_name));
      }

      if (audioStream != null) {
        video.setAudioCodec(AudioCodec.fromString(audioStream.codec_name));
        video.setAudioChannels(audioStream.channels);
        video.setAudioSampleRate(audioStream.sample_rate);
      }

      video.setDuration(probeResult.getFormat().duration);
      return video;

    } catch (IOException e) {
      log.error("Error extracting metadata from video: {}", file.getAbsolutePath(), e);
      throw new UncheckedIOException(e);
    }
  }
}
