package dev.razafindratelo.arsmedia.service.event;

import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.service.UserService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoFormatConversionRequestedService {
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;

  private final BucketComponent bucketComponent;
  private final VideoRepository videoRepository;
  private final UserService userService;

  public VideoFormatConversionRequestedService(
      @Value("${ffmpeg.path}") String ffmpegPath,
      @Value("${ffprobe.path}") String ffprobePath,
      BucketComponent bucketComponent,
      VideoRepository videoRepository,
      UserService userService)
      throws IOException {
    this.ffmpeg = new FFmpeg(ffmpegPath);
    this.ffprobe = new FFprobe(ffprobePath);
    this.bucketComponent = bucketComponent;
    this.videoRepository = videoRepository;
    this.userService = userService;
  }
}
