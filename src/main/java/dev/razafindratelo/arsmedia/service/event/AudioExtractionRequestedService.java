package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.AudioMapper.toJAudio;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.lang.String.format;

import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.exception.AudioExtractionException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.file.TempFileCleaner;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.AudioRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JAudio;
import dev.razafindratelo.arsmedia.repository.model.job.AudioExtractionJob;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AudioExtractionRequestedService implements Consumer<AudioExtractionRequested> {

  private static final String EXTRACTED_AUDIO_PREFIX = "audio_extracted_";
  private static final String AUDIO_FILE_EXTENSION = ".mp3";
  private static final String AUDIO_FORMAT = "mp3";
  private static final String AUDIO_CODEC = "libmp3lame";
  private static final int AUDIO_QUALITY_HIGH = 1;
  private static final int NO_AUDIO_CHANNELS = 0;
  private static final int NO_AUDIO_SAMPLE_RATE = 0;
  private static final int ZERO_BITRATE = 0;

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository videoRepository;
  private final AudioRepository audioRepository;
  private final UserService userService;
  private final AudioExtractionJobRepository jobRepository;
  private final TempFileCleaner tempFileCleaner;
  private final BitRateCalculator bitRateCalculator;

  public AudioExtractionRequestedService(
      @Value("${ffmpeg.path}") String ffmpegPath,
      @Value("${ffprobe.path}") String ffprobePath,
      BucketComponent bucketComponent,
      VideoRepository videoRepository,
      AudioRepository audioRepository,
      UserService userService,
      AudioExtractionJobRepository jobRepository,
      TempFileCleaner tempFileCleaner,
      BitRateCalculator bitRateCalculator)
      throws IOException {

    this.ffmpeg = new FFmpeg(ffmpegPath);
    this.ffprobe = new FFprobe(ffprobePath);
    this.bucketComponent = bucketComponent;
    this.videoRepository = videoRepository;
    this.audioRepository = audioRepository;
    this.userService = userService;
    this.jobRepository = jobRepository;
    this.tempFileCleaner = tempFileCleaner;
    this.bitRateCalculator = bitRateCalculator;
  }

  @Override
  public void accept(AudioExtractionRequested event) {
    String jobId = event.getJobId();

    logExtractionStart(event);

    try {
      updateJobStatus(jobId, ProcessStatus.PROGRESSING, event.getAttemptNb(), null);
      processAudioExtraction(event);
      logExtractionSuccess(event);
    } catch (Exception e) {
      handleExtractionError(event, e);
    }
  }

  private void processAudioExtraction(AudioExtractionRequested event) throws IOException {
    Video sourceVideo = fetchSourceVideo(event.getVideoId());
    var owner = userService.findByEmail(event.getOwner());

    validateVideoHasAudio(sourceVideo);

    File videoFile = downloadVideoFile(event.getBucketKey());
    File audioFile = null;

    try {
      audioFile = extractAudio(videoFile, sourceVideo);
      String audioBucketKey = uploadExtractedAudio(audioFile, event.getVideoId());

      Audio audio = buildAudioEntity(sourceVideo, audioFile, audioBucketKey, owner);
      saveExtractedAudio(audio, event.getJobId());

    } finally {
      tempFileCleaner.cleanUp(videoFile, audioFile);
    }
  }

  private Video fetchSourceVideo(String videoId) {
    return toVideo(
        videoRepository
            .findById(videoId)
            .orElseThrow(() -> new EntityNotFoundException("Video not found: " + videoId)));
  }

  private void validateVideoHasAudio(Video video) {
    boolean hasNoAudio =
        video.getAudioChannels() <= NO_AUDIO_CHANNELS
            || video.getAudioSampleRate() <= NO_AUDIO_SAMPLE_RATE;

    if (hasNoAudio) {
      throw new IllegalArgumentException("Video does not contain an audio track");
    }

    log.info(
        "Video has audio: {} channels at {} Hz",
        video.getAudioChannels(),
        video.getAudioSampleRate());
  }

  private File downloadVideoFile(String bucketKey) {
    log.info("Downloading video from bucket key: {}", bucketKey);
    File file = bucketComponent.download(bucketKey);
    log.info("Video downloaded, size: {} bytes", file.length());
    return file;
  }

  private File extractAudio(File videoFile, Video sourceVideo) throws IOException {
    File audioFile = File.createTempFile(EXTRACTED_AUDIO_PREFIX, AUDIO_FILE_EXTENSION);
    log.info("Created temporary audio file: {}", audioFile.getAbsolutePath());

    FFmpegBuilder builder = buildAudioExtractionCommand(videoFile, audioFile, sourceVideo);
    executeAudioExtraction(builder);

    log.info("Audio extraction completed, size: {} bytes", audioFile.length());
    return audioFile;
  }

  private FFmpegBuilder buildAudioExtractionCommand(File input, File output, Video sourceVideo) {

    log.info(
        "Configuring audio extraction: {} channels at {} Hz",
        sourceVideo.getAudioChannels(),
        sourceVideo.getAudioSampleRate());

    return new FFmpegBuilder()
        .setInput(input.getAbsolutePath())
        .overrideOutputFiles(true)
        .addOutput(output.getAbsolutePath())
        .setFormat(AUDIO_FORMAT)
        .setAudioCodec(AUDIO_CODEC)
        .setAudioQuality(AUDIO_QUALITY_HIGH)
        .setAudioChannels(sourceVideo.getAudioChannels())
        .setAudioSampleRate(sourceVideo.getAudioSampleRate())
        .disableVideo()
        .disableSubtitle()
        .done();
  }

  private void executeAudioExtraction(FFmpegBuilder builder) {
    log.info("Starting FFmpeg audio extraction");
    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();
    log.info("FFmpeg audio extraction completed");
  }

  private String uploadExtractedAudio(File audioFile, String videoId) {
    String bucketKey = generateAudioBucketKey(videoId);
    log.info("Uploading extracted audio to bucket key: {}", bucketKey);
    bucketComponent.upload(audioFile, bucketKey);
    log.info("Extracted audio uploaded successfully");
    return bucketKey;
  }

  private String generateAudioBucketKey(String videoId) {
    return EXTRACTED_AUDIO_PREFIX
        + videoId
        + "_"
        + System.currentTimeMillis()
        + AUDIO_FILE_EXTENSION;
  }

  private Audio buildAudioEntity(Video sourceVideo, File audioFile, String bucketKey, User owner) {
    Audio audio = new Audio();

    audio.setId(UUID.randomUUID().toString());
    audio.setOwner(owner);
    audio.setFileName(generateAudioFileName(sourceVideo.getFileName()));
    audio.setSize(audioFile.length());
    audio.setSizeType(SizeType.BYTES);
    audio.setFileType(FileType.AUDIO);
    audio.setCreatedAt(LocalDateTime.now());
    audio.setFilePath(bucketKey);
    audio.setDuration(sourceVideo.getDuration());
    audio.setCodec(AudioCodec.MP3);
    audio.setChannels(sourceVideo.getAudioChannels());
    audio.setSampleRate(sourceVideo.getAudioSampleRate());
    audio.setFormat(ContainerFormat.MP3);
    audio.setBitRate((int) bitRateCalculator.calculate(audioFile, sourceVideo.getDuration()));

    log.info(
        "Created audio entity: {} channels, {} Hz, {} bytes",
        audio.getChannels(),
        audio.getSampleRate(),
        audioFile.length());

    return audio;
  }

  private String generateAudioFileName(String originalVideoFileName) {
    String nameWithoutExtension = originalVideoFileName.replaceAll("\\.[^.]+$", "");
    return EXTRACTED_AUDIO_PREFIX + nameWithoutExtension + AUDIO_FILE_EXTENSION;
  }

  private void saveExtractedAudio(Audio audio, String jobId) {
    log.info("Saving extracted audio entity to database");
    JAudio savedAudio = audioRepository.save(toJAudio(audio));

    AudioExtractionJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new EntityNotFoundException("Audio extraction job not found: " + jobId));

    job.setStatus(ProcessStatus.COMPLETED);
    job.setExtractedAudio(savedAudio);
    job.setCompletedAt(LocalDateTime.now());
    jobRepository.save(job);

    log.info("Updated audio extraction job {} status to: COMPLETED", jobId);
  }

  private void updateJobStatus(
      String jobId, ProcessStatus status, int attemptCount, String errorMessage) {
    try {
      AudioExtractionJob job =
          jobRepository
              .findById(jobId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Audio extraction job not found: " + jobId));

      job.setStatus(status);
      job.setAttemptCount(attemptCount);

      if (errorMessage != null) {
        job.setErrorMessage(errorMessage);
      }

      if (status == ProcessStatus.COMPLETED || status == ProcessStatus.FAILED) {
        job.setCompletedAt(LocalDateTime.now());
      }

      jobRepository.save(job);
      log.info("Updated audio extraction job {} status to: {}", jobId, status);

    } catch (Exception e) {
      log.error("Failed to update audio extraction job status for job_id: {}", jobId, e);
    }
  }

  private void logExtractionStart(AudioExtractionRequested event) {
    log.info(
        "Processing audio extraction event for video: {}, bucket key: {}, job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getBucketKey(),
        event.getJobId(),
        event.getAttemptNb());
  }

  private void logExtractionSuccess(AudioExtractionRequested event) {
    log.info(
        "Audio extraction completed successfully for video: {}, job_id: {}",
        event.getVideoId(),
        event.getJobId());
  }

  private void handleExtractionError(AudioExtractionRequested event, Exception e) {
    log.error(
        "Audio extraction failed for video: {}, job_id: {}, attempt: {}",
        event.getVideoId(),
        event.getJobId(),
        event.getAttemptNb(),
        e);

    String errorMessage =
        format("Audio extraction failed on attempt %d: %s", event.getAttemptNb(), e.getMessage());

    updateJobStatus(event.getJobId(), ProcessStatus.FAILED, event.getAttemptNb(), errorMessage);
    throw new AudioExtractionException("Audio extraction processing failed", e);
  }
}
