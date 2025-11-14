package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.AudioMapper.toJAudio;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;

import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.exception.AudioExtractionException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.*;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.AudioRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.JAudio;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

  private static final String PREFIX = "audio_extracted_";
  private static final String TMP_EXTENSION = ".mp3";
  private static final String FORMAT_MP3 = "mp3";
  private static final String CODEC_MP3 = "libmp3lame";
  private static final String LOG_JOB_NOT_FOUND = "Audio extraction job not found: ";
  private static final String LOG_VIDEO_NOT_FOUND = "Video not found: ";
  private static final String LOG_NO_AUDIO_TRACK = "Video does not contain audio track";
  private static final String LOG_ERROR_UPDATING_JOB =
      "Failed to update audio extraction job status for job_id: {}";

  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final BucketComponent bucketComponent;
  private final VideoRepository videoRepository;
  private final AudioRepository audioRepository;
  private final UserService userService;
  private final AudioExtractionJobRepository audioExtractionJobRepository;

  public AudioExtractionRequestedService(
      @Value("${ffmpeg.path}") String ffmpegPath,
      @Value("${ffprobe.path}") String ffprobePath,
      BucketComponent bucketComponent,
      VideoRepository videoRepository,
      AudioRepository audioRepository,
      UserService userService,
      AudioExtractionJobRepository audioExtractionJobRepository)
      throws IOException {

    this.ffmpeg = new FFmpeg(ffmpegPath);
    this.ffprobe = new FFprobe(ffprobePath);

    this.bucketComponent = bucketComponent;
    this.videoRepository = videoRepository;
    this.audioRepository = audioRepository;
    this.userService = userService;
    this.audioExtractionJobRepository = audioExtractionJobRepository;
  }

  @Override
  public void accept(AudioExtractionRequested event) {
    String jobId = event.getJobId();

    try {
      log.info(
          "Processing audio extraction event for video: {}, bucket key: {}, job_id: {}, attempt:"
              + " {}",
          event.getVideoId(),
          event.getBucketKey(),
          jobId,
          event.getAttemptNb());

      updateJobStatus(jobId, ProcessStatus.PROGRESSING, event.getAttemptNb(), null);

      processAudioExtraction(event, jobId);

      log.info(
          "Audio extraction completed successfully for video: {}, job_id: {}",
          event.getVideoId(),
          jobId);

    } catch (Exception e) {
      log.error(
          "Audio extraction failed for video: {}, job_id: {}, attempt: {}",
          event.getVideoId(),
          jobId,
          event.getAttemptNb(),
          e);

      String errorMessage =
          "Audio extraction failed on attempt %d: %s"
              .formatted(event.getAttemptNb(), e.getMessage());

      updateJobStatus(jobId, ProcessStatus.FAILED, event.getAttemptNb(), errorMessage);

      throw new AudioExtractionException("Audio extraction processing failed", e);
    }
  }

  private void updateJobStatus(
      String jobId, ProcessStatus status, int attemptCount, String errorMessage) {

    try {
      AudioExtractionJob job =
          audioExtractionJobRepository
              .findById(jobId)
              .orElseThrow(() -> new EntityNotFoundException(LOG_JOB_NOT_FOUND + jobId));

      job.setStatus(status);
      job.setAttemptCount(attemptCount);

      if (errorMessage != null) {
        job.setErrorMessage(errorMessage);
      }

      if (status == ProcessStatus.COMPLETED || status == ProcessStatus.FAILED) {
        job.setCompletedAt(LocalDateTime.now());
      }

      audioExtractionJobRepository.save(job);
      log.info("Updated audio extraction job {} status to: {}", jobId, status);

    } catch (Exception e) {
      log.error(LOG_ERROR_UPDATING_JOB, jobId, e);
    }
  }

  private Audio processAudioExtraction(AudioExtractionRequested event, String jobId)
      throws IOException {

    Video originalVideo =
        toVideo(
            videoRepository
                .findById(event.getVideoId())
                .orElseThrow(
                    () -> new EntityNotFoundException(LOG_VIDEO_NOT_FOUND + event.getVideoId())));

    var owner = userService.findByEmail(event.getOwner());

    if (originalVideo.getAudioChannels() <= 0 || originalVideo.getAudioSampleRate() <= 0) {
      throw new IllegalArgumentException(LOG_NO_AUDIO_TRACK);
    }

    log.info("Downloading original video from bucket key: {}", event.getBucketKey());
    File originalFile = bucketComponent.download(event.getBucketKey());
    log.info("Original video downloaded, size: {} bytes", originalFile.length());

    log.info("Starting audio extraction for video: {}", event.getVideoId());
    File extractedAudioFile = extractAudioFile(originalFile, originalVideo);
    log.info("Audio extraction completed, extracted size: {} bytes", extractedAudioFile.length());

    String audioBucketKey = generateAudioBucketKey(event.getVideoId());
    log.info("Uploading extracted audio to bucket key: {}", audioBucketKey);
    bucketComponent.upload(extractedAudioFile, audioBucketKey);
    log.info("Extracted audio uploaded successfully");

    Audio audio = createAudioEntity(originalVideo, extractedAudioFile, audioBucketKey);

    audio.setId(UUID.randomUUID().toString());
    audio.setOwner(owner);

    log.info("Extracted audio size: {}{}", audio.getSize(), audio.getSizeType());
    log.info("Saving extracted audio entity to database");

    JAudio savedAudio = audioRepository.save(toJAudio(audio));

    AudioExtractionJob job =
        audioExtractionJobRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException(LOG_JOB_NOT_FOUND + jobId));

    job.setStatus(ProcessStatus.COMPLETED);
    job.setExtractedAudio(savedAudio);
    job.setCompletedAt(LocalDateTime.now());
    audioExtractionJobRepository.save(job);

    log.info("Updated audio extraction job {} status to: COMPLETED", jobId);

    cleanupTempFiles(originalFile, extractedAudioFile);
    log.info("Temporary files cleaned up");

    return audio;
  }

  private File extractAudioFile(File originalFile, Video originalVideo) throws IOException {
    File audioFile = File.createTempFile(PREFIX, TMP_EXTENSION);
    log.info("Created temporary output file: {}", audioFile.getAbsolutePath());

    log.info(
        "Extracting audio with {} channels and {} sample rate",
        originalVideo.getAudioChannels(),
        originalVideo.getAudioSampleRate());

    FFmpegBuilder builder =
        new FFmpegBuilder()
            .setInput(originalFile.getAbsolutePath())
            .overrideOutputFiles(true)
            .addOutput(audioFile.getAbsolutePath())
            .setFormat(FORMAT_MP3)
            .setAudioCodec(CODEC_MP3)
            .setAudioQuality(1)
            .setAudioChannels(originalVideo.getAudioChannels())
            .setAudioSampleRate(originalVideo.getAudioSampleRate())
            .disableVideo()
            .disableSubtitle()
            .done();

    log.info("Starting FFmpeg audio extraction");
    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
    executor.createJob(builder).run();
    log.info("FFmpeg audio extraction completed");

    return audioFile;
  }

  private String generateAudioBucketKey(String videoId) {
    return PREFIX + videoId + "_" + System.currentTimeMillis() + TMP_EXTENSION;
  }

  private Audio createAudioEntity(Video originalVideo, File audioFile, String bucketKey) {
    Audio audio = new Audio();

    audio.setFileName(PREFIX + originalVideo.getFileName().replaceAll("\\.[^.]+$", TMP_EXTENSION));
    audio.setSize(audioFile.length());
    audio.setSizeType(SizeType.BYTES);
    audio.setFileType(FileType.AUDIO);
    audio.setCreatedAt(LocalDateTime.now());
    audio.setFilePath(bucketKey);

    audio.setDuration(originalVideo.getDuration());
    audio.setCodec(AudioCodec.MP3);
    audio.setChannels(originalVideo.getAudioChannels());
    audio.setSampleRate(originalVideo.getAudioSampleRate());
    audio.setFormat(ContainerFormat.MP3);
    audio.setBitRate(calculateAudioBitRate(audioFile, originalVideo.getDuration()));

    log.info(
        "Created audio entity: {} channels, {} Hz, {} bytes",
        audio.getChannels(),
        audio.getSampleRate(),
        audioFile.length());

    return audio;
  }

  private int calculateAudioBitRate(File file, double durationInSeconds) {
    if (durationInSeconds <= 0) return 0;
    long fileSizeBits = file.length() * 8;
    return (int) (fileSizeBits / durationInSeconds / 1000);
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
