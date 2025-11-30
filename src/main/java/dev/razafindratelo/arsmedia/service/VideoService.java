package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.AudioExtractionJobStatusResponseMapper.mapToAudioExtractionJobStatusResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoCompressionJobStatusResponseMapper.mapToCompressionResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoFormatConversionJobStatusResponseMapper.mapToFormatConversionResponse;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.util.UUID.randomUUID;

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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
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
  private final FormatConversionValidator formatConversionValidator;

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

    return AudioExtractionJobStatusResponse.builder()
        .jobId(event.getJobId())
        .status(audioJob.getStatus())
        .createdAt(audioJob.getCreatedAt())
        .attemptCount(audioJob.getAttemptCount())
        .build();
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
    return VideoCompressionJob.builder()
        .id(randomUUID().toString())
        .parent(toJVideo(video))
        .createdAt(LocalDateTime.now())
        .status(ProcessStatus.PENDING)
        .attemptCount(0)
        .build();
  }

  private AudioExtractionJob buildAudioExtractionJob(Video video) {
    return AudioExtractionJob.builder()
        .id(randomUUID().toString())
        .parent(toJVideo(video))
        .createdAt(LocalDateTime.now())
        .status(ProcessStatus.PENDING)
        .attemptCount(0)
        .build();
  }

  public VideoFormatConversionJobStatusResponse convertTo(
      @NotNull ContainerFormat toFormat,
      @NotBlank @NotNull String bucketKey,
      @Email @NotNull @NotBlank String userEmail) {

    var video = fetchVideoAndValidateOwner(userEmail, bucketKey);

    var request =
        new FormatConversionValidator.FormatConversionRequest(video.getContainerFormat(), toFormat);
    var errors = new BeanPropertyBindingResult(request, "formatConversionRequest");

    formatConversionValidator.validate(request, errors);

    if (errors.hasErrors()) {
      throw new InvalidFormatConversionException(
          errors.getAllErrors().getFirst().getDefaultMessage());
    }

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

  private VideoFormatConversionJob buildFormatConversionJob(Video video) {
    return VideoFormatConversionJob.builder()
        .id(randomUUID().toString())
        .parent(toJVideo(video))
        .createdAt(LocalDateTime.now())
        .status(ProcessStatus.PENDING)
        .attemptCount(0)
        .build();
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
