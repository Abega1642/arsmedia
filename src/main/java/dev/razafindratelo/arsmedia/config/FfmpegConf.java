package dev.razafindratelo.arsmedia.config;

import java.io.IOException;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FfmpegConf {

  @Bean
  public FFmpeg ffmpeg(@Value("${ffmpeg.path}") String ffmpegPath) throws IOException {
    return new FFmpeg(ffmpegPath);
  }

  @Bean
  public FFprobe ffprobe(@Value("${ffprobe.path}") String ffprobePath) throws IOException {
    return new FFprobe(ffprobePath);
  }
}
