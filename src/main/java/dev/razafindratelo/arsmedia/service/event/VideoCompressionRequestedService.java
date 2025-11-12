package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.COMPLETED;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.PROGRESSING;
import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.exception.VideoProcessingException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.CompressedVideoRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VideoCompressionRequestedService implements Consumer<VideoCompressionRequested> {

  public static final String PREFIX = "compressed_";
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository repository;
  private final UserService userService;
  private final CompressedVideoRepository compressedVideoRepository;

  public VideoCompressionRequestedService(
      BucketComponent bucketComponent,
      VideoRepository repository,
      UserService userService,
      CompressedVideoRepository compressedVideoRepository)
      throws IOException {
    this.userService = userService;
    this.compressedVideoRepository = compressedVideoRepository;
    this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
    this.ffprobe = new FFprobe("/usr/bin/ffprobe");
    this.bucketComponent = bucketComponent;
    this.repository = repository;
  }

  @Override
  public void accept(VideoCompressionRequested event) {
    try {
      log.info(
          "Processing video compression event for video: {} and bucket key = {}",
          event.getVideoId(),
          event.getBucketKey());
      var compressedVideo = processCompression(event);
      compressedVideoRepository.updateCompressedVideoStatus(COMPLETED, compressedVideo.getId());
      log.info("Video compression completed successfully for: {}", event.getVideoId());

    } catch (Exception e) {
      log.error("Video compression failed for video: {}", event.getVideoId(), e);
      throw new VideoProcessingException("Video compression processing failed", e);
    }
  }

  private JCompressedVideo processCompression(VideoCompressionRequested event) throws IOException {
    Video originalVideo =
        toVideo(
            repository
                .findById(event.getVideoId())
                .orElseThrow(
                    () -> new EntityNotFoundException("Video not found: " + event.getVideoId())));

    var owner = userService.findByEmail(event.getOwner());

    log.info("Downloading original video from bucket key: {}", event.getBucketKey());
    File originalFile = bucketComponent.download(event.getBucketKey());
    log.info("Original video downloaded, size: {} bytes", originalFile.length());

    log.info("Starting video compression for video: {}", event.getVideoId());
    File compressedFile =
        createCompressedFile(originalFile, originalVideo, event.getCompressionOptions());
    log.info("Video compression completed, compressed size: {} bytes", compressedFile.length());

    String compressedBucketKey = generateCompressedBucketKey(event.getVideoId());
    log.info("Uploading compressed video to bucket key: {}", compressedBucketKey);
    bucketComponent.upload(compressedFile, compressedBucketKey);
    log.info("Compressed video uploaded successfully");

    Video compressedVideo =
        createCompressedVideoEntity(
            originalVideo, compressedFile, compressedBucketKey, event.getCompressionOptions());
    compressedVideo.setId(UUID.randomUUID().toString());
    compressedVideo.setOwner(owner);

    JCompressedVideo jCompressedVideo =
        new JCompressedVideo(
            UUID.randomUUID().toString(), toJVideo(compressedVideo), now(), PROGRESSING);

    log.info(
        "Compressed video size : {}{}", compressedVideo.getSize(), compressedVideo.getSizeType());

    log.info("Saving compressed video entity to database");
    repository.save(toJVideo(compressedVideo));

    cleanupTempFiles(originalFile, compressedFile);
    log.info("Temporary files cleaned up");
    return compressedVideoRepository.save(jCompressedVideo);
  }

  private File createCompressedFile(
      File originalFile, Video originalVideo, CompressionOptions options) throws IOException {

    File compressedFile = File.createTempFile(PREFIX, ".mp4");
    log.info("Created temporary output file: {}", compressedFile.getAbsolutePath());

    double frameRate = originalVideo.getFrameRate();
    if (frameRate <= 0) {
      frameRate = 30.0;
      log.info("Invalid frame rate detected, using default: {}", frameRate);
    } else {
      log.info("Using frame rate: {}", frameRate);
    }

    int targetWidth =
        options.getTargetWidth() != null ? options.getTargetWidth() : originalVideo.getWidth();
    int targetHeight =
        options.getTargetHeight() != null ? options.getTargetHeight() : originalVideo.getHeight();

    if (targetWidth % 2 != 0) {
      targetWidth = targetWidth - 1;
      log.info(
          "Adjusting width to be divisible by 2: {} -> {}", originalVideo.getWidth(), targetWidth);
    }
    if (targetHeight % 2 != 0) {
      targetHeight = targetHeight - 1;
      log.info(
          "Adjusting height to be divisible by 2: {} -> {}",
          originalVideo.getHeight(),
          targetHeight);
    }

    FFmpegBuilder builder =
        new FFmpegBuilder()
            .setInput(originalFile.getAbsolutePath())
            .overrideOutputFiles(true)
            .addOutput(compressedFile.getAbsolutePath())
            .setFormat("mp4")
            .setVideoCodec("libx264")
            .setConstantRateFactor(options.getCrf())
            .setVideoFrameRate(frameRate)
            .done();

    boolean hasAudio =
        originalVideo.getAudioChannels() > 0 && originalVideo.getAudioSampleRate() > 0;

    if (hasAudio) {
      log.info(
          "Video has audio with {} channels and {} sample rate",
          originalVideo.getAudioChannels(),
          originalVideo.getAudioSampleRate());
    } else {
      log.info("Video has no audio");
    }

    if (targetWidth != originalVideo.getWidth() || targetHeight != originalVideo.getHeight()) {
      log.info(
          "Rescaling video from {}x{} to {}x{}",
          originalVideo.getWidth(),
          originalVideo.getHeight(),
          targetWidth,
          targetHeight);
      builder.setVideoFilter("scale=" + targetWidth + ":" + targetHeight);
    } else {
      log.info("Keeping original resolution: {}x{}", targetWidth, targetHeight);
    }

    log.info("Starting FFmpeg compression with CRF: {}", options.getCrf());
    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();
    log.info("FFmpeg compression completed");

    return compressedFile;
  }

  private String generateCompressedBucketKey(String videoId) {
    return PREFIX + videoId + "_" + System.currentTimeMillis();
  }

  private Video createCompressedVideoEntity(
      Video originalVideo,
      File compressedFile,
      String compressedBucketKey,
      CompressionOptions options) {
    Video compressedVideo = new Video();

    int targetWidth =
        options.getTargetWidth() != null ? options.getTargetWidth() : originalVideo.getWidth();
    int targetHeight =
        options.getTargetHeight() != null ? options.getTargetHeight() : originalVideo.getHeight();

    if (targetWidth % 2 != 0) {
      targetWidth = targetWidth - 1;
    }
    if (targetHeight % 2 != 0) {
      targetHeight = targetHeight - 1;
    }

    boolean hasAudio =
        originalVideo.getAudioChannels() > 0 && originalVideo.getAudioSampleRate() > 0;

    double frameRate = originalVideo.getFrameRate();
    if (frameRate <= 0) {
      frameRate = 30.0;
    }

    compressedVideo.setFileName(PREFIX + originalVideo.getFileName());
    compressedVideo.setSize(compressedFile.length());
    compressedVideo.setSizeType(originalVideo.getSizeType());
    compressedVideo.setFileType(originalVideo.getFileType());
    compressedVideo.setCreatedAt(now());
    compressedVideo.setFilePath(compressedBucketKey);

    compressedVideo.setDuration(originalVideo.getDuration());
    compressedVideo.setCodec(VideoCodec.H264);
    compressedVideo.setWidth(targetWidth);
    compressedVideo.setHeight(targetHeight);
    compressedVideo.setFrameRate(frameRate);
    compressedVideo.setAspectRatio(calculateAspectRatio(targetWidth, targetHeight));
    compressedVideo.setContainerFormat(ContainerFormat.MP4);
    compressedVideo.setBitRate(calculateBitRate(compressedFile, originalVideo.getDuration()));

    if (hasAudio) {
      compressedVideo.setAudioCodec(AudioCodec.AAC);
      compressedVideo.setAudioChannels(originalVideo.getAudioChannels());
      compressedVideo.setAudioSampleRate(originalVideo.getAudioSampleRate());
    } else {
      compressedVideo.setAudioCodec(AudioCodec.NONE);
      compressedVideo.setAudioChannels(0);
      compressedVideo.setAudioSampleRate(0);
    }

    log.info(
        "Created compressed video entity: {}x{}, {} bytes, audio: {}",
        targetWidth,
        targetHeight,
        compressedFile.length(),
        hasAudio ? "yes" : "no");

    return compressedVideo;
  }

  private String calculateAspectRatio(int width, int height) {
    int gcd = findGCD(width, height);
    return (width / gcd) + ":" + (height / gcd);
  }

  private int findGCD(int a, int b) {
    return b == 0 ? a : findGCD(b, a % b);
  }

  private double calculateBitRate(File file, double durationInSeconds) {
    if (durationInSeconds <= 0) return 0;
    long fileSizeBits = file.length() * 8;
    return fileSizeBits / durationInSeconds / 1000;
  }

  private void cleanupTempFiles(File... files) {
    for (File file : files) {
      if (file != null && file.exists()) {
        try {
          Files.delete(file.toPath());
        } catch (IOException e) {
          log.warn(
              "Failed to delete temporary file: {}. Reason: {}",
              file.getAbsolutePath(),
              e.getMessage(),
              e);
        }
      }
    }
  }
}
