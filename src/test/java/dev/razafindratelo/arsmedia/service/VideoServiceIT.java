package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.*;
import dev.razafindratelo.arsmedia.repository.CompressedVideoRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Slf4j
class VideoServiceIT {

  public static final String TEST_USER_EMAIL = "user@example.com";
  @Mock private VideoRepository videoRepository;
  @Mock private UserService userService;
  @Mock private EventProducer<VideoCompressionRequested> eventProducer;
  @Mock private CompressedVideoRepository compressedVideoRepository;

  private VideoService videoService;

  @BeforeEach
  void setUp() {
    videoService =
        new VideoService(videoRepository, userService, eventProducer, compressedVideoRepository);
  }

  @Test
  void should_create_compression_job_with_default_options() {
    var bucketKey = "video_bucket_key_123";
    var videoId = UUID.randomUUID().toString();

    var mockVideo = createMockJVideo(videoId, bucketKey);
    var mockUser = createMockUser(TEST_USER_EMAIL);

    when(videoRepository.findByBucketKey(bucketKey)).thenReturn(Optional.of(mockVideo));
    when(userService.findByEmail(TEST_USER_EMAIL)).thenReturn(mockUser);
    when(compressedVideoRepository.save(any(JCompressedVideo.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CompressionJobStatusResponse response = videoService.compress(TEST_USER_EMAIL, bucketKey);

    assertNotNull(response);
    assertNotNull(response.getJobId());
    assertEquals(ProcessStatus.PENDING, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNull(response.getCompletedAt());
    assertNull(response.getCompressedVideoId());
    assertNull(response.getCompressedVideoUrl());
    assertNull(response.getErrorMessage());
    assertEquals(0, response.getAttemptCount());

    verify(videoRepository).findByBucketKey(bucketKey);
    verify(userService).findByEmail(TEST_USER_EMAIL);
    verify(compressedVideoRepository).save(any(JCompressedVideo.class));

    ArgumentCaptor<List<VideoCompressionRequested>> eventCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventCaptor.capture());

    List<VideoCompressionRequested> events = eventCaptor.getValue();
    assertEquals(1, events.size());
    VideoCompressionRequested event = events.getFirst();
    assertEquals(videoId, event.getVideoId());
    assertEquals(bucketKey, event.getBucketKey());
    assertEquals(TEST_USER_EMAIL, event.getOwner());
    assertEquals(response.getJobId(), event.getJobId());
    assertNotNull(event.getCompressionOptions());

    log.info("Compression job created successfully with job_id: {}", response.getJobId());
  }

  @Test
  void should_create_compression_job_with_custom_options() {
    var bucketKey = "video_bucket_key_456";
    var videoId = UUID.randomUUID().toString();
    CompressionOptions customOptions =
        CompressionOptions.builder().crf(28).targetWidth(1280).targetHeight(720).build();

    var mockVideo = createMockJVideo(videoId, bucketKey);
    var mockUser = createMockUser(TEST_USER_EMAIL);

    when(videoRepository.findByBucketKey(bucketKey)).thenReturn(Optional.of(mockVideo));
    when(userService.findByEmail(TEST_USER_EMAIL)).thenReturn(mockUser);
    when(compressedVideoRepository.save(any(JCompressedVideo.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CompressionJobStatusResponse response =
        videoService.requestCompressionWithOptions(TEST_USER_EMAIL, bucketKey, customOptions);

    assertNotNull(response);
    assertNotNull(response.getJobId());
    assertEquals(ProcessStatus.PENDING, response.getStatus());
    assertEquals(0, response.getAttemptCount());

    ArgumentCaptor<List<VideoCompressionRequested>> eventCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventCaptor.capture());

    List<VideoCompressionRequested> events = eventCaptor.getValue();
    VideoCompressionRequested event = events.getFirst();
    assertEquals(customOptions.getCrf(), event.getCompressionOptions().getCrf());
    assertEquals(customOptions.getTargetWidth(), event.getCompressionOptions().getTargetWidth());
    assertEquals(customOptions.getTargetHeight(), event.getCompressionOptions().getTargetHeight());

    log.info(
        "Compression job created with custom options - CRF: {}, Resolution: {}x{}",
        customOptions.getCrf(),
        customOptions.getTargetWidth(),
        customOptions.getTargetHeight());
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var bucketKey = "non_existent_key";

    when(videoRepository.findByBucketKey(bucketKey)).thenReturn(Optional.empty());

    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class, () -> videoService.compress(TEST_USER_EMAIL, bucketKey));

    assertTrue(exception.getMessage().contains("No video instance found"));
    assertTrue(exception.getMessage().contains(bucketKey));

    verify(videoRepository).findByBucketKey(bucketKey);
    verify(userService, never()).findByEmail(any());
    verify(compressedVideoRepository, never()).save(any());
    verify(eventProducer, never()).accept(any());

    log.info("Correctly threw exception for non-existent video with bucket_key: {}", bucketKey);
  }

  @Test
  void should_retrieve_compression_job_status() {
    var jobId = UUID.randomUUID().toString();
    var videoId = UUID.randomUUID().toString();
    var compressedVideoId = UUID.randomUUID().toString();

    JVideo mockParentVideo = createMockJVideo(videoId, "original_key");
    JVideo mockCompressedVideo = createMockJVideo(compressedVideoId, "compressed_key");
    mockCompressedVideo.setBucketKey("s3://bucket/compressed_video.mp4");

    JCompressedVideo mockJob = new JCompressedVideo();
    mockJob.setId(jobId);
    mockJob.setParent(mockParentVideo);
    mockJob.setCompressedVideo(mockCompressedVideo);
    mockJob.setStatus(ProcessStatus.COMPLETED);
    mockJob.setCreatedAt(LocalDateTime.now().minusMinutes(5));
    mockJob.setCompletedAt(LocalDateTime.now());
    mockJob.setAttemptCount(1);
    mockJob.setErrorMessage(null);

    when(compressedVideoRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

    CompressionJobStatusResponse response = videoService.getCompressionStatus(jobId);

    assertNotNull(response);
    assertEquals(jobId, response.getJobId());
    assertEquals(ProcessStatus.COMPLETED, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNotNull(response.getCompletedAt());
    assertEquals(compressedVideoId, response.getCompressedVideoId());
    assertEquals("s3://bucket/compressed_video.mp4", response.getCompressedVideoUrl());
    assertNull(response.getErrorMessage());
    assertEquals(1, response.getAttemptCount());

    verify(compressedVideoRepository).findById(jobId);

    log.info(
        "Retrieved compression job status: {} - Status: {}, Attempts: {}",
        jobId,
        response.getStatus(),
        response.getAttemptCount());
  }

  @Test
  void should_retrieve_all_compression_jobs_for_video() {
    var videoId = UUID.randomUUID().toString();
    var mockParentVideo = createMockJVideo(videoId, "original_key");

    JCompressedVideo job1 =
        createMockCompressedJob(
            UUID.randomUUID().toString(),
            mockParentVideo,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1));

    JCompressedVideo job2 =
        createMockCompressedJob(
            UUID.randomUUID().toString(),
            mockParentVideo,
            ProcessStatus.FAILED,
            LocalDateTime.now().minusMinutes(30),
            LocalDateTime.now().minusMinutes(25));
    job2.setErrorMessage("FFmpeg processing failed");
    job2.setAttemptCount(3);

    JCompressedVideo job3 =
        createMockCompressedJob(
            UUID.randomUUID().toString(),
            mockParentVideo,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(5),
            null);

    List<JCompressedVideo> mockJobs = List.of(job1, job2, job3);

    when(compressedVideoRepository.findByParentId(videoId)).thenReturn(mockJobs);

    List<CompressionJobStatusResponse> responses =
        videoService.getCompressionJobsByVideoId(videoId);

    assertNotNull(responses);
    assertEquals(3, responses.size());

    CompressionJobStatusResponse response1 = responses.getFirst();
    assertEquals(job1.getId(), response1.getJobId());
    assertEquals(ProcessStatus.COMPLETED, response1.getStatus());
    assertNotNull(response1.getCompletedAt());
    assertNull(response1.getErrorMessage());

    CompressionJobStatusResponse response2 = responses.get(1);
    assertEquals(job2.getId(), response2.getJobId());
    assertEquals(ProcessStatus.FAILED, response2.getStatus());
    assertEquals("FFmpeg processing failed", response2.getErrorMessage());
    assertEquals(3, response2.getAttemptCount());

    CompressionJobStatusResponse response3 = responses.get(2);
    assertEquals(job3.getId(), response3.getJobId());
    assertEquals(ProcessStatus.PROGRESSING, response3.getStatus());
    assertNull(response3.getCompletedAt());

    verify(compressedVideoRepository).findByParentId(videoId);

    log.info(
        "Retrieved {} compression jobs for video {}: COMPLETED={}, FAILED={}, PROGRESSING={}",
        responses.size(),
        videoId,
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.COMPLETED).count(),
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.FAILED).count(),
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.PROGRESSING).count());
  }

  private JVideo createMockJVideo(String id, String bucketKey) {
    Video video = new Video();
    video.setId(id);
    video.setFileName("test_video.mp4");
    video.setFilePath(bucketKey);
    video.setWidth(1920);
    video.setHeight(1080);
    video.setDuration(120.0);
    video.setFrameRate(30.0);
    video.setSize(10_000_000L);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(2);
    video.setAudioSampleRate(48000);
    video.setAudioCodec(AudioCodec.AAC);
    video.setCreatedAt(LocalDateTime.now());
    video.setOwner(createMockUser(TEST_USER_EMAIL));
    return toJVideo(video);
  }

  private User createMockUser(String email) {
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setEmail(email);
    return user;
  }

  private JCompressedVideo createMockCompressedJob(
      String jobId,
      JVideo parentVideo,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt) {
    JCompressedVideo job = new JCompressedVideo();
    job.setId(jobId);
    job.setParent(parentVideo);
    job.setStatus(status);
    job.setCreatedAt(createdAt);
    job.setCompletedAt(completedAt);
    job.setAttemptCount(status == ProcessStatus.COMPLETED ? 1 : 0);

    if (status == ProcessStatus.COMPLETED) {
      JVideo compressedVideo = createMockJVideo(UUID.randomUUID().toString(), "compressed_key");
      compressedVideo.setBucketKey("s3://bucket/compressed_" + jobId + ".mp4");
      job.setCompressedVideo(compressedVideo);
    }

    return job;
  }
}
