package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
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

  public List<VideoCompressionJobStatusResponse> getCompressionJobsByVideoId(
      @NotBlank @NotNull String videoId) {
    return videoCompressionJobRepository.findByParentId(videoId).stream()
        .map(this::mapToCompressionResponse)
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

  private VideoCompressionJobStatusResponse mapToCompressionResponse(VideoCompressionJob job) {
    return new VideoCompressionJobStatusResponse(
        job.getId(),
        job.getStatus(),
        job.getCreatedAt(),
        job.getCompletedAt(),
        job.getCompressedVideo() != null ? job.getCompressedVideo().getId() : null,
        job.getCompressedVideo() != null ? toVideo(job.getCompressedVideo()).getFilePath() : null,
        job.getErrorMessage(),
        job.getAttemptCount());
  }
}
