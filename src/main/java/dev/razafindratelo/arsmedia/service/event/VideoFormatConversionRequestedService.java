package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import dev.razafindratelo.arsmedia.event.model.VideoFormatConversionRequested;
import dev.razafindratelo.arsmedia.exception.InvalidFormatConversionException;
import dev.razafindratelo.arsmedia.exception.VideoFormatConversionException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.file.TempFileCleaner;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.VideoFormatConversionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.repository.model.job.VideoFormatConversionJob;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VideoFormatConversionRequestedService
    implements Consumer<VideoFormatConversionRequested> {

  private static final String CONVERTED_PREFIX = "converted_";
  private static final String TEMP_FILE_PREFIX = "converted_video_";

  private static final Set<ContainerFormat> VIDEO_FORMATS =
      EnumSet.of(
          ContainerFormat.MP4,
          ContainerFormat.MKV,
          ContainerFormat.MOV,
          ContainerFormat.AVI,
          ContainerFormat.FLV,
          ContainerFormat.WMV,
          ContainerFormat.WEBM,
          ContainerFormat.MPEG_TS,
          ContainerFormat.MPEG_PS,
          ContainerFormat.THREEGP,
          ContainerFormat.ASF);

  private static final Set<ContainerFormat> AUDIO_FORMATS =
      EnumSet.of(
          ContainerFormat.MP3,
          ContainerFormat.OGG,
          ContainerFormat.M4A,
          ContainerFormat.WAV,
          ContainerFormat.FLAC,
          ContainerFormat.APE,
          ContainerFormat.AIFF);

  private static final String MP4_FILE_EXTENSION = ".mp4";
  private static final Map<ContainerFormat, String> FORMAT_EXTENSIONS =
      Map.ofEntries(
          Map.entry(ContainerFormat.MP4, MP4_FILE_EXTENSION),
          Map.entry(ContainerFormat.MKV, ".mkv"),
          Map.entry(ContainerFormat.MOV, ".mov"),
          Map.entry(ContainerFormat.AVI, ".avi"),
          Map.entry(ContainerFormat.FLV, ".flv"),
          Map.entry(ContainerFormat.WMV, ".wmv"),
          Map.entry(ContainerFormat.WEBM, ".webm"),
          Map.entry(ContainerFormat.MPEG_TS, ".ts"),
          Map.entry(ContainerFormat.MPEG_PS, ".mpg"),
          Map.entry(ContainerFormat.THREEGP, ".3gp"),
          Map.entry(ContainerFormat.ASF, ".asf"),
          Map.entry(ContainerFormat.MP3, ".mp3"),
          Map.entry(ContainerFormat.OGG, ".ogg"),
          Map.entry(ContainerFormat.M4A, ".m4a"),
          Map.entry(ContainerFormat.WAV, ".wav"),
          Map.entry(ContainerFormat.FLAC, ".flac"),
          Map.entry(ContainerFormat.APE, ".ape"),
          Map.entry(ContainerFormat.AIFF, ".aiff"));

  private static final String MP3_CODEC_STRING = "mp3";
  private static final Map<ContainerFormat, String> FORMAT_NAMES =
      Map.ofEntries(
          Map.entry(ContainerFormat.MP4, "mp4"),
          Map.entry(ContainerFormat.MKV, "matroska"),
          Map.entry(ContainerFormat.MOV, "mov"),
          Map.entry(ContainerFormat.AVI, "avi"),
          Map.entry(ContainerFormat.FLV, "flv"),
          Map.entry(ContainerFormat.WMV, "asf"),
          Map.entry(ContainerFormat.WEBM, "webm"),
          Map.entry(ContainerFormat.MPEG_TS, "mpegts"),
          Map.entry(ContainerFormat.MPEG_PS, "mpeg"),
          Map.entry(ContainerFormat.THREEGP, "3gp"),
          Map.entry(ContainerFormat.ASF, "asf"),
          Map.entry(ContainerFormat.MP3, MP3_CODEC_STRING),
          Map.entry(ContainerFormat.OGG, "ogg"),
          Map.entry(ContainerFormat.M4A, "ipod"),
          Map.entry(ContainerFormat.WAV, "wav"),
          Map.entry(ContainerFormat.FLAC, "flac"),
          Map.entry(ContainerFormat.APE, "ape"),
          Map.entry(ContainerFormat.AIFF, "aiff"));

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository videoRepository;
  private final UserService userService;
  private final VideoFormatConversionJobRepository jobRepository;
  private final TempFileCleaner tempFileCleaner;
  private final BitRateCalculator bitRateCalculator;

  public VideoFormatConversionRequestedService(
      @Value("${ffmpeg.path}") String ffmpegPath,
      @Value("${ffprobe.path}") String ffprobePath,
      BucketComponent bucketComponent,
      VideoRepository videoRepository,
      UserService userService,
      VideoFormatConversionJobRepository jobRepository,
      TempFileCleaner tempFileCleaner,
      BitRateCalculator bitRateCalculator)
      throws IOException {
    this.ffmpeg = new FFmpeg(ffmpegPath);
    this.ffprobe = new FFprobe(ffprobePath);
    this.bucketComponent = bucketComponent;
    this.videoRepository = videoRepository;
    this.userService = userService;
    this.jobRepository = jobRepository;
    this.tempFileCleaner = tempFileCleaner;
    this.bitRateCalculator = bitRateCalculator;
  }

  @Override
  public void accept(VideoFormatConversionRequested event) {
    String jobId = event.getJobId();

    logConversionStart(event);

    try {
      updateJobStatus(jobId, ProcessStatus.PROGRESSING, event.getAttemptNb(), null);
      processFormatConversion(event);
      logConversionSuccess(event);
    } catch (Exception e) {
      handleConversionError(event, e);
    }
  }

  private void processFormatConversion(VideoFormatConversionRequested event) throws IOException {
    Video originalVideo = fetchOriginalVideo(event.getVideoId());
    User owner = userService.findByEmail(event.getOwner());

    ContainerFormat targetFormat = event.getTargetFormat();
    validateFormatConversion(originalVideo.getContainerFormat(), targetFormat);

    File originalFile = downloadOriginalVideo(event.getBucketKey());
    File convertedFile = null;

    try {
      convertedFile = convertVideoFormat(originalFile, targetFormat);
      String convertedBucketKey =
          uploadConvertedVideo(convertedFile, event.getVideoId(), targetFormat);

      Video convertedVideo =
          buildConvertedVideoEntity(
              originalVideo, convertedFile, convertedBucketKey, targetFormat, owner);

      saveConvertedVideo(convertedVideo, event.getJobId());

    } finally {
      tempFileCleaner.cleanUp(originalFile, convertedFile);
    }
  }

  private void validateFormatConversion(
      ContainerFormat sourceFormat, ContainerFormat targetFormat) {
    if (targetFormat == ContainerFormat.UNKNOWN) {
      throw new InvalidFormatConversionException("Target format is unknown or unsupported");
    }

    if (sourceFormat == targetFormat) {
      throw new InvalidFormatConversionException(
          String.format("Source and target formats are the same: %s", sourceFormat));
    }

    boolean sourceIsVideo = VIDEO_FORMATS.contains(sourceFormat);
    boolean targetIsVideo = VIDEO_FORMATS.contains(targetFormat);
    boolean sourceIsAudio = AUDIO_FORMATS.contains(sourceFormat);
    boolean targetIsAudio = AUDIO_FORMATS.contains(targetFormat);

    if (sourceIsVideo && targetIsAudio) {
      throw new InvalidFormatConversionException(
          String.format(
              "Cannot convert video format %s to audio format %s. Use audio extraction instead.",
              sourceFormat, targetFormat));
    }

    if (sourceIsAudio && targetIsVideo) {
      throw new InvalidFormatConversionException(
          String.format(
              "Cannot convert audio format %s to video format %s", sourceFormat, targetFormat));
    }

    if (!FORMAT_NAMES.containsKey(targetFormat)) {
      throw new InvalidFormatConversionException(
          String.format("Target format %s is not supported for conversion", targetFormat));
    }

    log.info("Format conversion validated: {} -> {}", sourceFormat, targetFormat);
  }

  private Video fetchOriginalVideo(String videoId) {
    return toVideo(
        videoRepository
            .findById(videoId)
            .orElseThrow(() -> new EntityNotFoundException("Video not found: " + videoId)));
  }

  private File downloadOriginalVideo(String bucketKey) {
    log.info("Downloading original video from bucket key: {}", bucketKey);
    File file = bucketComponent.download(bucketKey);
    log.info("Original video downloaded, size: {} bytes", file.length());
    return file;
  }

  private File convertVideoFormat(File originalFile, ContainerFormat targetFormat)
      throws IOException {
    String extension = FORMAT_EXTENSIONS.getOrDefault(targetFormat, MP4_FILE_EXTENSION);
    File convertedFile = File.createTempFile(TEMP_FILE_PREFIX, extension);
    log.info("Created temporary output file: {}", convertedFile.getAbsolutePath());

    FFmpegBuilder builder = buildConversionCommand(originalFile, convertedFile, targetFormat);
    executeConversion(builder);

    log.info("Format conversion completed, output size: {} bytes", convertedFile.length());
    return convertedFile;
  }

  private FFmpegBuilder buildConversionCommand(
      File input, File output, ContainerFormat targetFormat) {
    String formatName = FORMAT_NAMES.get(targetFormat);

    log.info("Building FFmpeg command for format conversion to: {}", formatName);

    var builder =
        new FFmpegBuilder()
            .setInput(input.getAbsolutePath())
            .overrideOutputFiles(true)
            .addOutput(output.getAbsolutePath())
            .setFormat(formatName);

    if (canCopyStreams(targetFormat)) {
      builder.setVideoCodec("copy").setAudioCodec("copy");
      log.info("Using stream copy (no re-encoding) for faster conversion");
    } else {
      configureCodecsForFormat(builder, targetFormat);
    }

    return builder.done();
  }

  private boolean canCopyStreams(ContainerFormat targetFormat) {
    return targetFormat == ContainerFormat.MP4
        || targetFormat == ContainerFormat.MKV
        || targetFormat == ContainerFormat.MOV
        || targetFormat == ContainerFormat.MPEG_TS;
  }

  private void configureCodecsForFormat(FFmpegOutputBuilder builder, ContainerFormat targetFormat) {
    switch (targetFormat) {
      case WEBM:
        builder.setVideoCodec("libvpx-vp9").setAudioCodec("libopus");
        log.info("Configured codecs for WEBM: VP9 video, Opus audio");
        break;
      case FLV:
        builder.setVideoCodec("flv").setAudioCodec(MP3_CODEC_STRING);
        log.info("Configured codecs for FLV: FLV video, MP3 audio");
        break;
      case AVI:
        builder.setVideoCodec("mpeg4").setAudioCodec(MP3_CODEC_STRING);
        log.info("Configured codecs for AVI: MPEG-4 video, MP3 audio");
        break;
      case THREEGP:
        builder.setVideoCodec("h263").setAudioCodec("amr_nb");
        log.info("Configured codecs for 3GP: H.263 video, AMR-NB audio");
        break;
      default:
        builder.setVideoCodec("copy").setAudioCodec("copy");
        log.info("Using default stream copy for format: {}", targetFormat);
        break;
    }
  }

  private void executeConversion(FFmpegBuilder builder) {
    log.info("Starting FFmpeg format conversion");
    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();
    log.info("FFmpeg format conversion completed");
  }

  private String uploadConvertedVideo(
      File convertedFile, String videoId, ContainerFormat targetFormat) {
    String bucketKey = generateConvertedBucketKey(videoId, targetFormat);
    log.info("Uploading converted video to bucket key: {}", bucketKey);
    bucketComponent.upload(convertedFile, bucketKey);
    log.info("Converted video uploaded successfully, size: {} bytes", convertedFile.length());
    return bucketKey;
  }

  private String generateConvertedBucketKey(String videoId, ContainerFormat targetFormat) {
    String extension = FORMAT_EXTENSIONS.getOrDefault(targetFormat, MP4_FILE_EXTENSION);
    return CONVERTED_PREFIX
        + videoId
        + "_"
        + targetFormat.name().toLowerCase()
        + "_"
        + System.currentTimeMillis()
        + extension;
  }

  private Video buildConvertedVideoEntity(
      Video original,
      File convertedFile,
      String bucketKey,
      ContainerFormat targetFormat,
      User owner) {

    Video converted = new Video();
    converted.setId(randomUUID().toString());
    converted.setOwner(owner);
    converted.setFileName(generateConvertedFileName(original.getFileName(), targetFormat));
    converted.setSize(convertedFile.length());
    converted.setSizeType(original.getSizeType());
    converted.setFileType(original.getFileType());
    converted.setCreatedAt(now());
    converted.setFilePath(bucketKey);

    converted.setDuration(original.getDuration());
    converted.setWidth(original.getWidth());
    converted.setHeight(original.getHeight());
    converted.setFrameRate(original.getFrameRate());
    converted.setAspectRatio(original.getAspectRatio());

    converted.setContainerFormat(targetFormat);

    converted.setCodec(determineVideoCodec(targetFormat, original.getCodec()));
    converted.setAudioCodec(determineAudioCodec(targetFormat, original.getAudioCodec()));

    converted.setAudioChannels(original.getAudioChannels());
    converted.setAudioSampleRate(original.getAudioSampleRate());
    converted.setBitRate(bitRateCalculator.calculate(convertedFile, original.getDuration()));

    log.info(
        "Created converted video entity: format={}, size={} bytes",
        targetFormat,
        convertedFile.length());

    return converted;
  }

  private String generateConvertedFileName(String originalFileName, ContainerFormat targetFormat) {
    String nameWithoutExtension = originalFileName.replaceAll("\\.[^.]+$", "");
    String extension = FORMAT_EXTENSIONS.getOrDefault(targetFormat, MP4_FILE_EXTENSION);
    return CONVERTED_PREFIX + nameWithoutExtension + extension;
  }

  private VideoCodec determineVideoCodec(ContainerFormat format, VideoCodec originalCodec) {
    return switch (format) {
      case WEBM -> VideoCodec.VP9;
      case FLV -> VideoCodec.FLV;
      case AVI -> VideoCodec.MPEG4;
      case THREEGP -> VideoCodec.H263;
      case MP4, MOV -> VideoCodec.H264;
      default -> originalCodec != null ? originalCodec : VideoCodec.H264;
    };
  }

  private AudioCodec determineAudioCodec(ContainerFormat format, AudioCodec originalCodec) {
    return switch (format) {
      case WEBM -> AudioCodec.OPUS;
      case FLV, AVI -> AudioCodec.MP3;
      case THREEGP -> AudioCodec.AMR_NB;
      case MP4, MOV -> AudioCodec.AAC;
      default -> originalCodec != null ? originalCodec : AudioCodec.AAC;
    };
  }

  private void saveConvertedVideo(Video convertedVideo, String jobId) {
    log.info("Saving converted video entity to database");
    JVideo savedVideo = videoRepository.save(toJVideo(convertedVideo));

    VideoFormatConversionJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new EntityNotFoundException("Format conversion job not found: " + jobId));

    job.setStatus(ProcessStatus.COMPLETED);
    job.setConvertedVideo(savedVideo);
    job.setCompletedAt(now());
    jobRepository.save(job);

    log.info("Updated format conversion job {} status to: COMPLETED", jobId);
  }

  private void updateJobStatus(
      String jobId, ProcessStatus status, int attemptCount, String errorMessage) {
    try {
      VideoFormatConversionJob job =
          jobRepository
              .findById(jobId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Format conversion job not found: " + jobId));

      job.setAttemptCount(attemptCount);
      log.info("Video Format conversion attempt={}", attemptCount);
      job.setStatus(status);

      if (errorMessage != null) job.setErrorMessage(errorMessage);

      if (status == ProcessStatus.COMPLETED || status == ProcessStatus.FAILED)
        job.setCompletedAt(now());

      jobRepository.save(job);
      log.info("Updated format conversion job {} status to: {}", jobId, status);
    } catch (Exception e) {
      log.error("Failed to update format conversion job status for job_id: {}", jobId, e);
    }
  }

  private void logConversionStart(VideoFormatConversionRequested event) {
    log.info(
        "Processing video format conversion event for video: {}, bucket key: {}, target format: {},"
            + " job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getBucketKey(),
        event.getTargetFormat(),
        event.getJobId(),
        event.getAttemptNb());
  }

  private void logConversionSuccess(VideoFormatConversionRequested event) {
    log.info(
        "Video format conversion completed successfully for video: {}, target format: {}, job_id:"
            + " {}",
        event.getVideoId(),
        event.getTargetFormat(),
        event.getJobId());
  }

  private void handleConversionError(VideoFormatConversionRequested event, Exception e) {
    log.error(
        "Video format conversion failed for video: {}, target format: {}, job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getTargetFormat(),
        event.getJobId(),
        event.getAttemptNb(),
        e);

    String errorMessage =
        String.format(
            "Format conversion to %s failed on attempt %d: %s",
            event.getTargetFormat(), event.getAttemptNb(), e.getMessage());

    updateJobStatus(event.getJobId(), ProcessStatus.FAILED, event.getAttemptNb(), errorMessage);
    throw new VideoFormatConversionException("Video format conversion processing failed", e);
  }
}
