package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
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

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository repository;

  public VideoCompressionRequestedService(
      BucketComponent bucketComponent, VideoRepository repository) throws IOException {
    this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
    this.ffprobe = new FFprobe("/usr/bin/ffprobe");
    this.bucketComponent = bucketComponent;
    this.repository = repository;
  }

  @Override
  public void accept(VideoCompressionRequested event) {
    try {
      log.info("Processing video compression event for video: {}", event.getVideoId());
      processCompression(event);
      log.info("Video compression completed successfully for: {}", event.getVideoId());

    } catch (Exception e) {
      log.error("Video compression failed for video: {}", event.getVideoId(), e);
      throw new RuntimeException("Video compression processing failed", e);
    }
  }

  private void processCompression(VideoCompressionRequested event) throws IOException {
    Video originalVideo =
        toVideo(
            repository
                .findById(event.getVideoId())
                .orElseThrow(
                    () -> new EntityNotFoundException("Video not found: " + event.getVideoId())));

    File originalFile = bucketComponent.download(event.getBucketKey());
    File compressedFile =
        createCompressedFile(originalFile, originalVideo, event.getCompressionOptions());

    String compressedBucketKey = generateCompressedBucketKey(event.getVideoId());
    bucketComponent.upload(compressedFile, compressedBucketKey);

    Video compressedVideo =
        createCompressedVideoEntity(
            originalVideo, compressedFile, compressedBucketKey, event.getCompressionOptions());

    repository.save(toJVideo(compressedVideo));

    cleanupTempFiles(originalFile, compressedFile);
  }

  private File createCompressedFile(
      File originalFile, Video originalVideo, CompressionOptions options) throws IOException {

    File compressedFile = File.createTempFile("compressed_", ".mp4");

    FFmpegBuilder builder =
        new FFmpegBuilder()
            .setInput(originalFile.getAbsolutePath())
            .overrideOutputFiles(true)
            .addOutput(compressedFile.getAbsolutePath())
            .setFormat("mp4")
            .setVideoCodec("libx264")
            .setConstantRateFactor(options.getCrf())
            .setVideoFrameRate(originalVideo.getFrameRate())
            .setAudioCodec("aac")
            .setAudioChannels(originalVideo.getAudioChannels())
            .setAudioSampleRate(originalVideo.getAudioSampleRate())
            .done();

    int targetWidth =
        options.getTargetWidth() != null ? options.getTargetWidth() : originalVideo.getWidth();
    int targetHeight =
        options.getTargetHeight() != null ? options.getTargetHeight() : originalVideo.getHeight();

    if (targetWidth != originalVideo.getWidth() || targetHeight != originalVideo.getHeight()) {
      builder.setVideoFilter("scale=" + targetWidth + ":" + targetHeight);
    }

    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();

    return compressedFile;
  }

  private String generateCompressedBucketKey(String videoId) {
    return "compressed_" + videoId + "_" + System.currentTimeMillis();
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

    compressedVideo.setFileName("compressed_" + originalVideo.getFileName());
    compressedVideo.setSize(compressedFile.length());
    compressedVideo.setSizeType(originalVideo.getSizeType());
    compressedVideo.setFileType(originalVideo.getFileType());
    compressedVideo.setCreatedAt(LocalDateTime.now());
    compressedVideo.setFilePath(compressedBucketKey);

    compressedVideo.setDuration(originalVideo.getDuration());
    compressedVideo.setCodec(VideoCodec.H264);
    compressedVideo.setWidth(targetWidth);
    compressedVideo.setHeight(targetHeight);
    compressedVideo.setFrameRate(originalVideo.getFrameRate());
    compressedVideo.setAspectRatio(calculateAspectRatio(targetWidth, targetHeight));
    compressedVideo.setContainerFormat(ContainerFormat.MP4);
    compressedVideo.setBitRate(calculateBitRate(compressedFile, originalVideo.getDuration()));
    compressedVideo.setAudioCodec(AudioCodec.AAC);
    compressedVideo.setAudioChannels(originalVideo.getAudioChannels());
    compressedVideo.setAudioSampleRate(originalVideo.getAudioSampleRate());

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
    return (double) fileSizeBits / durationInSeconds / 1000;
  }

  private void cleanupTempFiles(File... files) {
    for (File file : files) {
      if (file != null && file.exists()) {
        if (!file.delete()) {
          log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
        }
      }
    }
  }
}
