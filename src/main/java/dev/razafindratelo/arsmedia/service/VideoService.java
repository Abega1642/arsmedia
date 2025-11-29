package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.AudioExtractionJobStatusResponseMapper.mapToAudioExtractionJobStatusResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoCompressionJobStatusResponseMapper.mapToCompressionResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoFormatConversionJobStatusResponseMapper.mapToFormatConversionResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoFormatConversionJobStatusResponse;
import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.event.model.VideoFormatConversionRequested;
import dev.razafindratelo.arsmedia.exception.InvalidFormatConversionException;
import dev.razafindratelo.arsmedia.mapper.VideoCompressionJobStatusResponseMapper;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoFormatConversionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.job.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.job.VideoCompressionJob;
import dev.razafindratelo.arsmedia.repository.model.job.VideoFormatConversionJob;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
@Slf4j
public class VideoService {

  private final VideoRepository repository;
  private final UserService userService;
  private final EventProducer<VideoCompressionRequested> videoCompressionRequestedEventProducer;
  private final EventProducer<AudioExtractionRequested> audioExtractionRequestedEventProducer;
  private final VideoCompressionJobRepository videoCompressionJobRepository;
  private final AudioExtractionJobRepository audioExtractionJobRepository;
  private final EventProducer<VideoFormatConversionRequested>
      videoFormatConversionRequestedEventProducer;
  private final VideoFormatConversionJobRepository videoFormatConversionJobRepository;

  public VideoCompressionJobStatusResponse compress(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    return requestCompression(email, bucketKey, CompressionOptions.defaults());
  }

  public VideoCompressionJobStatusResponse requestCompressionWithOptions(
      @NotBlank @Email @NotNull String email,
      @NotBlank @NotNull String bucketKey,
      @NotNull CompressionOptions options) {
    return requestCompression(email, bucketKey, options);
  }

  private VideoCompressionJobStatusResponse requestCompression(
      String email, String bucketKey, CompressionOptions options) {
    var video = fetchVideoAndValidateOwner(email, bucketKey);

    var compressionJob = buildCompressionJob(video);
    videoCompressionJobRepository.save(compressionJob);

    var event =
        new VideoCompressionRequested(
            video.getId(), bucketKey, email, options, compressionJob.getId());
    videoCompressionRequestedEventProducer.accept(List.of(event));

    log.info(
        "Video compression event sent for video: {}, job_id: {}",
        video.getId(),
        compressionJob.getId());

    return mapToCompressionResponse(compressionJob);
  }

  public VideoCompressionJobStatusResponse getCompressionStatus(@NotBlank @NotNull String jobId) {
    var job =
        videoCompressionJobRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Compression job not found: " + jobId));
    return mapToCompressionResponse(job);
  }

  public AudioExtractionJobStatusResponse getAudioExtractionStatus(
      @NotBlank @NotNull String jobId) {
    var job =
        audioExtractionJobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new EntityNotFoundException("Audio extraction job not found: " + jobId));

    return mapToAudioExtractionJobStatusResponse(job);
  }

  public List<VideoCompressionJobStatusResponse> getCompressionJobsByVideoId(
      @NotBlank @NotNull String videoId) {
    return videoCompressionJobRepository.findByParentId(videoId).stream()
        .map(VideoCompressionJobStatusResponseMapper::mapToCompressionResponse)
        .collect(Collectors.toList());
  }

  public AudioExtractionJobStatusResponse extractAudio(
      @Email @NotBlank String userEmail, @NotNull @NotBlank String bucketKey) {
    var video = fetchVideoAndValidateOwner(userEmail, bucketKey);

    var audioJob = buildAudioExtractionJob(video);
    audioExtractionJobRepository.save(audioJob);

    var event =
        new AudioExtractionRequested(
            video.getId(), bucketKey, video.getOwner().getEmail(), audioJob.getId());
    audioExtractionRequestedEventProducer.accept(List.of(event));

    log.info(
        "Audio extraction event sent for video: {}, job_id: {}", video.getId(), audioJob.getId());

    return new AudioExtractionJobStatusResponse(
        event.getJobId(),
        audioJob.getStatus(),
        audioJob.getCreatedAt(),
        null,
        null,
        null,
        null,
        audioJob.getAttemptCount());
  }

  private Video fetchVideoAndValidateOwner(String email, String bucketKey) {
    var video =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("No video instance found with bucket_key = %s", bucketKey)));

    var user = userService.findByEmail(email);

    if (!video.getOwner().getEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException("Cannot perform action because of invalid information");
    }
    return toVideo(video);
  }

  private VideoCompressionJob buildCompressionJob(Video video) {
    var job = new VideoCompressionJob();
    job.setId(UUID.randomUUID().toString());
    job.setParent(toJVideo(video));
    job.setCreatedAt(LocalDateTime.now());
    job.setStatus(ProcessStatus.PENDING);
    job.setAttemptCount(0);
    return job;
  }

  private AudioExtractionJob buildAudioExtractionJob(Video video) {
    var job = new AudioExtractionJob();
    job.setId(UUID.randomUUID().toString());
    job.setParent(toJVideo(video));
    job.setCreatedAt(LocalDateTime.now());
    job.setStatus(ProcessStatus.PENDING);
    job.setAttemptCount(0);
    return job;
  }

  public VideoFormatConversionJobStatusResponse convertTo(
      @NotNull ContainerFormat toFormat,
      @NotBlank @NotNull String bucketKey,
      @Email @NotNull @NotBlank String userEmail) {

    var video = fetchVideoAndValidateOwner(userEmail, bucketKey);

    validateFormatConversion(video.getContainerFormat(), toFormat);

    var conversionJob = buildFormatConversionJob(video);
    videoFormatConversionJobRepository.save(conversionJob);

    var event =
        new VideoFormatConversionRequested(
            video.getId(), bucketKey, userEmail, toFormat, conversionJob.getId());

    videoFormatConversionRequestedEventProducer.accept(List.of(event));

    log.info(
        "Video format conversion event sent for video: {}, target format: {}, job_id: {}",
        video.getId(),
        toFormat,
        conversionJob.getId());

    return mapToFormatConversionResponse(conversionJob);
  }

  private void validateFormatConversion(
      ContainerFormat sourceFormat, ContainerFormat targetFormat) {
    if (targetFormat == ContainerFormat.UNKNOWN)
      throw new InvalidFormatConversionException("Target format is unknown or unsupported");

    if (sourceFormat == targetFormat)
      throw new InvalidFormatConversionException(
          String.format("Source and target formats are the same: %s", sourceFormat));

    Set<ContainerFormat> videoFormats =
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

    Set<ContainerFormat> audioFormats =
        EnumSet.of(
            ContainerFormat.MP3,
            ContainerFormat.OGG,
            ContainerFormat.M4A,
            ContainerFormat.WAV,
            ContainerFormat.FLAC,
            ContainerFormat.APE,
            ContainerFormat.AIFF);

    boolean sourceIsVideo = videoFormats.contains(sourceFormat);
    boolean targetIsVideo = videoFormats.contains(targetFormat);
    boolean sourceIsAudio = audioFormats.contains(sourceFormat);
    boolean targetIsAudio = audioFormats.contains(targetFormat);

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
  }

  private VideoFormatConversionJob buildFormatConversionJob(Video video) {
    var job = new VideoFormatConversionJob();
    job.setId(UUID.randomUUID().toString());
    job.setParent(toJVideo(video));
    job.setCreatedAt(LocalDateTime.now());
    job.setStatus(ProcessStatus.PENDING);
    job.setAttemptCount(0);
    return job;
  }

  public VideoFormatConversionJobStatusResponse getFormatConversionStatus(
      @NotBlank @NotNull String jobId) {
    var job =
        videoFormatConversionJobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new EntityNotFoundException("Format conversion job not found: " + jobId));
    return mapToFormatConversionResponse(job);
  }
}
