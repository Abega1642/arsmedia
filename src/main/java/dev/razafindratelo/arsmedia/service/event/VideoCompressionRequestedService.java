package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.exception.VideoProcessingException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.file.TempFileCleaner;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.job.VideoCompressionJob;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoCompressionRequestedService implements Consumer<VideoCompressionRequested> {

  private static final int NO_AUDIO_CHANNELS = 0;
  private static final int NO_AUDIO_SAMPLE_RATE = 0;
  private static final String COMPRESSED_PREFIX = "compressed_";
  private static final double DEFAULT_FRAME_RATE = 30.0;
  private static final String VIDEO_FORMAT = "mp4";
  private static final String VIDEO_CODEC = "libx264";
  private static final String TEMP_FILE_SUFFIX = ".mp4";

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository videoRepository;
  private final UserService userService;
  private final VideoCompressionJobRepository jobRepository;
  private final TempFileCleaner tempFileCleaner;
  private final BitRateCalculator bitRateCalculator;

  @Override
  public void accept(VideoCompressionRequested event) {
    String jobId = event.getJobId();

    logCompressionStart(event);

    try {
      updateJobStatus(jobId, ProcessStatus.PROGRESSING, event.getAttemptNb(), null);
      processCompression(event);
      logCompressionSuccess(event);
    } catch (Exception e) {
      handleCompressionError(event, e);
    }
  }

  private void processCompression(VideoCompressionRequested event) throws IOException {
    var originalVideo = fetchOriginalVideo(event.getVideoId());
    var owner = userService.findByEmail(event.getOwner());

    var originalFile = downloadOriginalVideo(event.getBucketKey());
    File compressedFile = null;

    try {
      compressedFile = compressVideo(originalFile, originalVideo, event.getCompressionOptions());
      String compressedBucketKey = uploadCompressedVideo(compressedFile, event.getVideoId());

      var compressedVideo =
          buildCompressedVideoEntity(
              originalVideo,
              compressedFile,
              compressedBucketKey,
              event.getCompressionOptions(),
              owner);

      saveCompressedVideo(compressedVideo, event.getJobId());

    } finally {
      tempFileCleaner.cleanUp(originalFile, compressedFile);
    }
  }

  private Video fetchOriginalVideo(String videoId) {
    return toVideo(
        videoRepository
            .findById(videoId)
            .orElseThrow(() -> new EntityNotFoundException("Video not found: " + videoId)));
  }

  private File downloadOriginalVideo(String bucketKey) {
    log.info("Downloading original video from bucket key: {}", bucketKey);
    var file = bucketComponent.download(bucketKey);
    log.info("Original video downloaded, size: {} bytes", file.length());

    return file;
  }

  private File compressVideo(File originalFile, Video originalVideo, CompressionOptions options)
      throws IOException {
    var compressedFile = File.createTempFile(COMPRESSED_PREFIX, TEMP_FILE_SUFFIX);
    log.info("Created temporary output file: {}", compressedFile.getAbsolutePath());

    VideoResolution resolution = calculateTargetResolution(originalVideo, options);
    double frameRate = determineFrameRate(originalVideo.getFrameRate());
    detectAudioPresence(originalVideo);

    FFmpegBuilder builder =
        buildFFmpegCommand(originalFile, compressedFile, resolution, frameRate, options.getCrf());

    executeCompression(builder);

    return compressedFile;
  }

  private VideoResolution calculateTargetResolution(Video video, CompressionOptions options) {
    int targetWidth =
        options.getTargetWidth() != null ? options.getTargetWidth() : video.getWidth();
    int targetHeight =
        options.getTargetHeight() != null ? options.getTargetHeight() : video.getHeight();

    targetWidth = makeEven(targetWidth);
    targetHeight = makeEven(targetHeight);

    if (targetWidth != video.getWidth() || targetHeight != video.getHeight()) {
      log.info(
          "Rescaling video from {}x{} to {}x{}",
          video.getWidth(),
          video.getHeight(),
          targetWidth,
          targetHeight);
    } else {
      log.info("Keeping original resolution: {}x{}", targetWidth, targetHeight);
    }

    return new VideoResolution(targetWidth, targetHeight);
  }

  private int makeEven(int value) {
    return (value % 2 != 0) ? value - 1 : value;
  }

  private double determineFrameRate(double originalFrameRate) {
    if (originalFrameRate <= 0) {
      log.info("Invalid frame rate detected, using default: {}", DEFAULT_FRAME_RATE);
      return DEFAULT_FRAME_RATE;
    }
    log.info("Using frame rate: {}", originalFrameRate);
    return originalFrameRate;
  }

  private boolean detectAudioPresence(Video video) {
    boolean hasAudio =
        video.getAudioChannels() > NO_AUDIO_CHANNELS
            && video.getAudioSampleRate() > NO_AUDIO_SAMPLE_RATE;
    if (hasAudio) {
      log.info(
          "Video has audio with {} channels and {} sample rate",
          video.getAudioChannels(),
          video.getAudioSampleRate());
    } else {
      log.info("Video has no audio");
    }
    return hasAudio;
  }

  private FFmpegBuilder buildFFmpegCommand(
      File input, File output, VideoResolution resolution, double frameRate, int crf) {

    FFmpegBuilder builder =
        new FFmpegBuilder()
            .setInput(input.getAbsolutePath())
            .overrideOutputFiles(true)
            .addOutput(output.getAbsolutePath())
            .setFormat(VIDEO_FORMAT)
            .setVideoCodec(VIDEO_CODEC)
            .setConstantRateFactor(crf)
            .setVideoFrameRate(frameRate)
            .done();

    if (resolution.isScaled())
      builder.setVideoFilter("scale=" + resolution.getWidth() + ":" + resolution.getHeight());

    return builder;
  }

  private void executeCompression(FFmpegBuilder builder) {
    log.info("Starting FFmpeg compression");
    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();
    log.info("FFmpeg compression completed");
  }

  private String uploadCompressedVideo(File compressedFile, String videoId) {
    String bucketKey = generateCompressedBucketKey(videoId);
    log.info("Uploading compressed video to bucket key: {}", bucketKey);
    bucketComponent.upload(compressedFile, bucketKey);
    log.info("Compressed video uploaded successfully, size: {} bytes", compressedFile.length());
    return bucketKey;
  }

  private String generateCompressedBucketKey(String videoId) {
    return COMPRESSED_PREFIX + videoId + "_" + System.currentTimeMillis();
  }

  private Video buildCompressedVideoEntity(
      Video original,
      File compressedFile,
      String bucketKey,
      CompressionOptions options,
      User owner) {

    VideoResolution resolution = calculateTargetResolution(original, options);
    double frameRate = determineFrameRate(original.getFrameRate());
    boolean hasAudio = detectAudioPresence(original);

    var compressed =
        Video.builder()
            .id(randomUUID().toString())
            .owner(owner)
            .fileName(COMPRESSED_PREFIX + original.getFileName())
            .size(compressedFile.length())
            .sizeType(original.getSizeType())
            .fileType(original.getFileType())
            .createdAt(now())
            .filePath(bucketKey)
            .duration(original.getDuration())
            .codec(VideoCodec.H264)
            .width(resolution.getWidth())
            .height(resolution.getHeight())
            .frameRate(frameRate)
            .aspectRatio(calculateAspectRatio(resolution.getWidth(), resolution.getHeight()))
            .containerFormat(ContainerFormat.MP4)
            .bitRate(bitRateCalculator.calculate(compressedFile, original.getDuration()))
            .build();

    setAudioProperties(compressed, original, hasAudio);

    log.info(
        "Created compressed video entity: {}x{}, {} bytes, audio: {}",
        resolution.getWidth(),
        resolution.getHeight(),
        compressedFile.length(),
        hasAudio ? "yes" : "no");

    return compressed;
  }

  private void setAudioProperties(Video compressed, Video original, boolean hasAudio) {
    if (hasAudio) {
      compressed.setAudioCodec(AudioCodec.AAC);
      compressed.setAudioChannels(original.getAudioChannels());
      compressed.setAudioSampleRate(original.getAudioSampleRate());
    } else {
      compressed.setAudioCodec(AudioCodec.NONE);
      compressed.setAudioChannels(NO_AUDIO_CHANNELS);
      compressed.setAudioSampleRate(NO_AUDIO_SAMPLE_RATE);
    }
  }

  private String calculateAspectRatio(int width, int height) {
    int gcd = findGCD(width, height);
    return (width / gcd) + ":" + (height / gcd);
  }

  private int findGCD(int a, int b) {
    return b == 0 ? a : findGCD(b, a % b);
  }

  private void saveCompressedVideo(Video compressedVideo, String jobId) {
    log.info("Saving compressed video entity to database");
    var savedVideo = videoRepository.save(toJVideo(compressedVideo));

    VideoCompressionJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Compression job not found: " + jobId));

    job.setStatus(ProcessStatus.COMPLETED);
    job.setCompressedVideo(savedVideo);
    job.setCompletedAt(now());
    jobRepository.save(job);

    log.info("Updated compression job {} status to: COMPLETED", jobId);
  }

  private void updateJobStatus(
      String jobId, ProcessStatus status, int attemptCount, String errorMessage) {
    try {
      VideoCompressionJob job =
          jobRepository
              .findById(jobId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Compression job not found: " + jobId));

      job.setAttemptCount(attemptCount);
      job.setStatus(status);

      if (errorMessage != null) job.setErrorMessage(errorMessage);

      if (status == ProcessStatus.COMPLETED || status == ProcessStatus.FAILED)
        job.setCompletedAt(now());

      jobRepository.save(job);
      log.info("Updated compression job {} status to: {}", jobId, status);
    } catch (Exception e) {
      log.error("Failed to update compression job status for job_id: {}", jobId, e);
    }
  }

  private void logCompressionStart(VideoCompressionRequested event) {
    log.info(
        "Processing video compression event for video: {}, bucket key: {}, job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getBucketKey(),
        event.getJobId(),
        event.getAttemptNb());
  }

  private void logCompressionSuccess(VideoCompressionRequested event) {
    log.info(
        "Video compression completed successfully for video: {}, job_id: {}",
        event.getVideoId(),
        event.getJobId());
  }

  private void handleCompressionError(VideoCompressionRequested event, Exception e) {
    log.error(
        "Video compression failed for video: {}, job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getJobId(),
        event.getAttemptNb(),
        e);

    var errorMessage =
        String.format("Compression failed on attempt %d: %s", event.getAttemptNb(), e.getMessage());

    updateJobStatus(event.getJobId(), ProcessStatus.FAILED, event.getAttemptNb(), errorMessage);
    throw new VideoProcessingException("Video compression processing failed", e);
  }

  @Getter
  private static class VideoResolution {
    private final int width;
    private final int height;
    private final boolean scaled;

    public VideoResolution(int width, int height) {
      this.width = width;
      this.height = height;
      this.scaled = true;
    }
  }
}
