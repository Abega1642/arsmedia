package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.AudioMapper.toJAudio;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.JAudio;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
  private static final String TEST_USER_EMAIL = "user@example.com";
  private static final String BUCKET_KEY_PREFIX = "video_bucket_key_";
  private static final String TEST_BUCKET_KEY_123 = BUCKET_KEY_PREFIX + "123";
  private static final String TEST_BUCKET_KEY_456 = BUCKET_KEY_PREFIX + "456";
  private static final String NON_EXISTENT_KEY = "non_existent_key";

  private static final String TEST_VIDEO_FILENAME = "test_video.mp4";
  private static final String S3_BUCKET_PREFIX = "s3://bucket/";
  private static final String COMPRESSED_VIDEO_PREFIX = "compressed_";
  private static final String COMPRESSED_KEY = "compressed_key";
  private static final String ORIGINAL_KEY = "original_key";

  private static final int VIDEO_WIDTH = 1920;
  private static final int VIDEO_HEIGHT = 1080;
  private static final double VIDEO_DURATION = 120.0;
  private static final double VIDEO_FRAME_RATE = 30.0;
  private static final long VIDEO_SIZE = 10_000_000L;
  private static final int AUDIO_CHANNELS = 2;
  private static final int AUDIO_SAMPLE_RATE = 48000;

  private static final int CUSTOM_CRF = 28;
  private static final int CUSTOM_WIDTH = 1280;
  private static final int CUSTOM_HEIGHT = 720;

  private static final String TEST_AUDIO_FILENAME = "test_audio.mp3";
  private static final double AUDIO_DURATION = 180.0;
  private static final int AUDIO_BIT_RATE = 320000;
  private static final long AUDIO_SIZE = 7_200_000L;

  private static final String NO_VIDEO_FOUND_MSG = "No video instance found";
  private static final String FFMPEG_ERROR_MSG = "FFmpeg processing failed";

  private static final String LOG_JOB_CREATED =
      "Compression job created successfully with job_id: {}";
  private static final String LOG_CUSTOM_OPTIONS =
      "Compression job created with custom options - CRF: {}, Resolution: {}x{}";
  private static final String LOG_EXCEPTION_THROWN =
      "Correctly threw exception for non-existent video with bucket_key: {}";
  private static final String LOG_STATUS_RETRIEVED =
      "Retrieved compression job status: {} - Status: {}, Attempts: {}";
  private static final String LOG_JOBS_RETRIEVED =
      "Retrieved {} compression jobs for video {}: COMPLETED={}, FAILED={}, PROGRESSING={}";

  @Mock private VideoRepository videoRepository;
  @Mock private UserService userService;
  @Mock private EventProducer<VideoCompressionRequested> vcEventProducer;
  @Mock private EventProducer<AudioExtractionRequested> aeEventProducer;
  @Mock private VideoCompressionJobRepository videoCompressionJobRepository;
  @Mock private AudioExtractionJobRepository audioExtractionJobRepository;

  private VideoService videoService;

  @BeforeEach
  void setUp() {
    videoService =
        new VideoService(
            videoRepository,
            userService,
            vcEventProducer,
            aeEventProducer,
            videoCompressionJobRepository,
            audioExtractionJobRepository);
  }

  @Test
  void should_create_compression_job_with_default_options() {
    var videoId = randomUUID().toString();
    var mockVideo = createMockJVideo(videoId, TEST_BUCKET_KEY_123);
    var mockUser = createMockUser();

    when(videoRepository.findByBucketKey(TEST_BUCKET_KEY_123)).thenReturn(Optional.of(mockVideo));
    when(userService.findByEmail(TEST_USER_EMAIL)).thenReturn(mockUser);
    when(videoCompressionJobRepository.save(any(VideoCompressionJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoCompressionJobStatusResponse response =
        videoService.compress(TEST_USER_EMAIL, TEST_BUCKET_KEY_123);

    assertCompressionJobResponse(response);

    verify(videoRepository).findByBucketKey(TEST_BUCKET_KEY_123);
    verify(userService).findByEmail(TEST_USER_EMAIL);
    verify(videoCompressionJobRepository).save(any(VideoCompressionJob.class));

    verifyCompressionEvent(response, videoId);

    log.info(LOG_JOB_CREATED, response.getJobId());
  }

  @Test
  void should_create_compression_job_with_custom_options() {
    var videoId = randomUUID().toString();
    CompressionOptions customOptions = createCustomCompressionOptions();

    var mockVideo = createMockJVideo(videoId, TEST_BUCKET_KEY_456);
    var mockUser = createMockUser();

    when(videoRepository.findByBucketKey(TEST_BUCKET_KEY_456)).thenReturn(Optional.of(mockVideo));
    when(userService.findByEmail(TEST_USER_EMAIL)).thenReturn(mockUser);
    when(videoCompressionJobRepository.save(any(VideoCompressionJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoCompressionJobStatusResponse response =
        videoService.requestCompressionWithOptions(
            TEST_USER_EMAIL, TEST_BUCKET_KEY_456, customOptions);

    assertNotNull(response);
    assertNotNull(response.getJobId());
    assertEquals(ProcessStatus.PENDING, response.getStatus());
    assertEquals(0, response.getAttemptCount());

    verifyCustomCompressionOptions(customOptions);

    log.info(
        LOG_CUSTOM_OPTIONS,
        customOptions.getCrf(),
        customOptions.getTargetWidth(),
        customOptions.getTargetHeight());
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    when(videoRepository.findByBucketKey(NON_EXISTENT_KEY)).thenReturn(Optional.empty());

    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> videoService.compress(TEST_USER_EMAIL, NON_EXISTENT_KEY));

    assertTrue(exception.getMessage().contains(NO_VIDEO_FOUND_MSG));
    assertTrue(exception.getMessage().contains(NON_EXISTENT_KEY));

    verify(videoRepository).findByBucketKey(NON_EXISTENT_KEY);
    verify(userService, never()).findByEmail(any());
    verify(videoCompressionJobRepository, never()).save(any());
    verify(vcEventProducer, never()).accept(any());

    log.info(LOG_EXCEPTION_THROWN, NON_EXISTENT_KEY);
  }

  @Test
  void should_retrieve_compression_job_status() {
    var jobId = randomUUID().toString();
    var videoId = randomUUID().toString();
    var compressedVideoId = randomUUID().toString();

    JVideo mockParentVideo = createMockJVideo(videoId, ORIGINAL_KEY);
    JVideo mockCompressedVideo = createMockJVideo(compressedVideoId, COMPRESSED_KEY);
    String compressedVideoUrl = buildCompressedVideoUrl("compressed_video.mp4");
    mockCompressedVideo.setBucketKey(compressedVideoUrl);

    VideoCompressionJob mockJob =
        createCompletedCompressionJob(jobId, mockParentVideo, mockCompressedVideo);

    when(videoCompressionJobRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

    VideoCompressionJobStatusResponse response = videoService.getCompressionStatus(jobId);

    assertCompletedJobResponse(response, jobId, compressedVideoId, compressedVideoUrl);

    verify(videoCompressionJobRepository).findById(jobId);

    log.info(LOG_STATUS_RETRIEVED, jobId, response.getStatus(), response.getAttemptCount());
  }

  @Test
  void should_retrieve_all_compression_jobs_for_video() {
    var videoId = randomUUID().toString();
    var mockParentVideo = createMockJVideo(videoId, ORIGINAL_KEY);

    VideoCompressionJob job1 =
        createMockCompressedJob(
            randomUUID().toString(),
            mockParentVideo,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1));

    VideoCompressionJob job2 =
        createFailedCompressionJob(
            randomUUID().toString(),
            mockParentVideo,
            LocalDateTime.now().minusMinutes(30),
            LocalDateTime.now().minusMinutes(25));

    VideoCompressionJob job3 =
        createMockCompressedJob(
            randomUUID().toString(),
            mockParentVideo,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(5),
            null);

    List<VideoCompressionJob> mockJobs = List.of(job1, job2, job3);

    when(videoCompressionJobRepository.findByParentId(videoId)).thenReturn(mockJobs);

    List<VideoCompressionJobStatusResponse> responses =
        videoService.getCompressionJobsByVideoId(videoId);

    assertMultipleJobResponses(responses, job1, job2, job3);

    verify(videoCompressionJobRepository).findByParentId(videoId);

    logJobsSummary(videoId, responses);
  }

  @Test
  void should_extract_audio_from_video() {
    var videoId = randomUUID().toString();
    var mockVideo = createMockJVideo(videoId, TEST_BUCKET_KEY_123);
    var mockUser = createMockUser();

    when(videoRepository.findByBucketKey(TEST_BUCKET_KEY_123)).thenReturn(Optional.of(mockVideo));
    when(userService.findByEmail(TEST_USER_EMAIL)).thenReturn(mockUser);
    when(audioExtractionJobRepository.save(any(AudioExtractionJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AudioExtractionJobStatusResponse response =
        videoService.extractAudio(TEST_USER_EMAIL, TEST_BUCKET_KEY_123);

    assertNotNull(response);
    assertNotNull(response.getJobId());
    assertEquals(ProcessStatus.PENDING, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNull(response.getCompletedAt());
    assertNull(response.getExtractedAudioId());
    assertNull(response.getExtractedAudioBucketKey());
    assertNull(response.getErrorMessage());
    assertEquals(0, response.getAttemptCount());

    verify(videoRepository).findByBucketKey(TEST_BUCKET_KEY_123);
    verify(userService).findByEmail(TEST_USER_EMAIL);
    verify(audioExtractionJobRepository).save(any(AudioExtractionJob.class));

    verifyAudioExtractionEvent(response, videoId);

    log.info("Audio extraction job created successfully with job_id: {}", response.getJobId());
  }

  @Test
  void should_retrieve_audio_extraction_job_status() {
    var jobId = randomUUID().toString();
    var videoId = randomUUID().toString();
    var extractedAudioId = randomUUID().toString();

    JVideo mockParentVideo = createMockJVideo(videoId, ORIGINAL_KEY);
    var mockExtractedAudio = createMockJAudio(extractedAudioId, "audio_key");
    String extractedAudioUrl = S3_BUCKET_PREFIX + "extracted_audio.mp3";
    mockExtractedAudio.setBucketKey(extractedAudioUrl);

    AudioExtractionJob mockJob =
        createCompletedAudioExtractionJob(jobId, mockParentVideo, mockExtractedAudio);

    when(audioExtractionJobRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

    AudioExtractionJobStatusResponse response = videoService.getAudioExtractionStatus(jobId);

    assertNotNull(response);
    assertEquals(jobId, response.getJobId());
    assertEquals(ProcessStatus.COMPLETED, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNotNull(response.getCompletedAt());
    assertEquals(extractedAudioId, response.getExtractedAudioId());
    assertEquals(extractedAudioUrl, response.getExtractedAudioBucketKey());
    assertNull(response.getErrorMessage());
    assertEquals(1, response.getAttemptCount());

    verify(audioExtractionJobRepository).findById(jobId);

    log.info(
        "Retrieved audio extraction job status: {} - Status: {}, Attempts: {}",
        jobId,
        response.getStatus(),
        response.getAttemptCount());
  }

  @Test
  void should_throw_exception_when_audio_extraction_job_not_found() {
    var nonExistentJobId = randomUUID().toString();

    when(audioExtractionJobRepository.findById(nonExistentJobId)).thenReturn(Optional.empty());

    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> videoService.getAudioExtractionStatus(nonExistentJobId));

    assertTrue(exception.getMessage().contains("Audio extraction job not found"));
    assertTrue(exception.getMessage().contains(nonExistentJobId));

    verify(audioExtractionJobRepository).findById(nonExistentJobId);
  }

  private void verifyAudioExtractionEvent(
      AudioExtractionJobStatusResponse response, String videoId) {
    ArgumentCaptor<List<AudioExtractionRequested>> eventCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(aeEventProducer).accept(eventCaptor.capture());

    List<AudioExtractionRequested> events = eventCaptor.getValue();
    assertEquals(1, events.size());
    AudioExtractionRequested event = events.getFirst();
    assertEquals(videoId, event.getVideoId());
    assertEquals(TEST_BUCKET_KEY_123, event.getBucketKey());
    assertEquals(TEST_USER_EMAIL, event.getOwner());
    assertEquals(response.getJobId(), event.getJobId());
  }

  private AudioExtractionJob createCompletedAudioExtractionJob(
      String jobId, JVideo parentVideo, JAudio extractedAudio) {
    AudioExtractionJob mockJob = new AudioExtractionJob();
    mockJob.setId(jobId);
    mockJob.setParent(parentVideo);
    mockJob.setExtractedAudio(extractedAudio);
    mockJob.setStatus(ProcessStatus.COMPLETED);
    mockJob.setCreatedAt(LocalDateTime.now().minusMinutes(5));
    mockJob.setCompletedAt(LocalDateTime.now());
    mockJob.setAttemptCount(1);
    mockJob.setErrorMessage(null);
    return mockJob;
  }

  private void assertCompressionJobResponse(VideoCompressionJobStatusResponse response) {
    assertNotNull(response);
    assertNotNull(response.getJobId());
    assertEquals(ProcessStatus.PENDING, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNull(response.getCompletedAt());
    assertNull(response.getCompressedVideoId());
    assertNull(response.getCompressedVideoUrl());
    assertNull(response.getErrorMessage());
    assertEquals(0, response.getAttemptCount());
  }

  private void assertCompletedJobResponse(
      VideoCompressionJobStatusResponse response,
      String jobId,
      String compressedVideoId,
      String compressedVideoUrl) {
    assertNotNull(response);
    assertEquals(jobId, response.getJobId());
    assertEquals(ProcessStatus.COMPLETED, response.getStatus());
    assertNotNull(response.getCreatedAt());
    assertNotNull(response.getCompletedAt());
    assertEquals(compressedVideoId, response.getCompressedVideoId());
    assertEquals(compressedVideoUrl, response.getCompressedVideoUrl());
    assertNull(response.getErrorMessage());
    assertEquals(1, response.getAttemptCount());
  }

  private void assertMultipleJobResponses(
      List<VideoCompressionJobStatusResponse> responses,
      VideoCompressionJob job1,
      VideoCompressionJob job2,
      VideoCompressionJob job3) {
    assertNotNull(responses);
    assertEquals(3, responses.size());

    VideoCompressionJobStatusResponse response1 = responses.getFirst();
    assertEquals(job1.getId(), response1.getJobId());
    assertEquals(ProcessStatus.COMPLETED, response1.getStatus());
    assertNotNull(response1.getCompletedAt());
    assertNull(response1.getErrorMessage());

    VideoCompressionJobStatusResponse response2 = responses.get(1);
    assertEquals(job2.getId(), response2.getJobId());
    assertEquals(ProcessStatus.FAILED, response2.getStatus());
    assertEquals(FFMPEG_ERROR_MSG, response2.getErrorMessage());
    assertEquals(3, response2.getAttemptCount());

    VideoCompressionJobStatusResponse response3 = responses.get(2);
    assertEquals(job3.getId(), response3.getJobId());
    assertEquals(ProcessStatus.PROGRESSING, response3.getStatus());
    assertNull(response3.getCompletedAt());
  }

  private void verifyCompressionEvent(VideoCompressionJobStatusResponse response, String videoId) {
    ArgumentCaptor<List<VideoCompressionRequested>> eventCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(vcEventProducer).accept(eventCaptor.capture());

    List<VideoCompressionRequested> events = eventCaptor.getValue();
    assertEquals(1, events.size());
    VideoCompressionRequested event = events.getFirst();
    assertEquals(videoId, event.getVideoId());
    assertEquals(VideoServiceIT.TEST_BUCKET_KEY_123, event.getBucketKey());
    assertEquals(TEST_USER_EMAIL, event.getOwner());
    assertEquals(response.getJobId(), event.getJobId());
    assertNotNull(event.getCompressionOptions());
  }

  private void verifyCustomCompressionOptions(CompressionOptions customOptions) {
    ArgumentCaptor<List<VideoCompressionRequested>> eventCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(vcEventProducer).accept(eventCaptor.capture());

    List<VideoCompressionRequested> events = eventCaptor.getValue();
    VideoCompressionRequested event = events.getFirst();
    assertEquals(customOptions.getCrf(), event.getCompressionOptions().getCrf());
    assertEquals(customOptions.getTargetWidth(), event.getCompressionOptions().getTargetWidth());
    assertEquals(customOptions.getTargetHeight(), event.getCompressionOptions().getTargetHeight());
  }

  private void logJobsSummary(String videoId, List<VideoCompressionJobStatusResponse> responses) {
    log.info(
        LOG_JOBS_RETRIEVED,
        responses.size(),
        videoId,
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.COMPLETED).count(),
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.FAILED).count(),
        responses.stream().filter(r -> r.getStatus() == ProcessStatus.PROGRESSING).count());
  }

  private CompressionOptions createCustomCompressionOptions() {
    return CompressionOptions.builder()
        .crf(CUSTOM_CRF)
        .targetWidth(CUSTOM_WIDTH)
        .targetHeight(CUSTOM_HEIGHT)
        .build();
  }

  private String buildCompressedVideoUrl(String filename) {
    return S3_BUCKET_PREFIX + filename;
  }

  private VideoCompressionJob createCompletedCompressionJob(
      String jobId, JVideo parentVideo, JVideo compressedVideo) {
    VideoCompressionJob mockJob = new VideoCompressionJob();
    mockJob.setId(jobId);
    mockJob.setParent(parentVideo);
    mockJob.setCompressedVideo(compressedVideo);
    mockJob.setStatus(ProcessStatus.COMPLETED);
    mockJob.setCreatedAt(LocalDateTime.now().minusMinutes(5));
    mockJob.setCompletedAt(LocalDateTime.now());
    mockJob.setAttemptCount(1);
    mockJob.setErrorMessage(null);
    return mockJob;
  }

  private VideoCompressionJob createFailedCompressionJob(
      String jobId, JVideo parentVideo, LocalDateTime createdAt, LocalDateTime completedAt) {
    VideoCompressionJob job =
        createMockCompressedJob(jobId, parentVideo, ProcessStatus.FAILED, createdAt, completedAt);
    job.setErrorMessage(FFMPEG_ERROR_MSG);
    job.setAttemptCount(3);
    return job;
  }

  private JVideo createMockJVideo(String id, String bucketKey) {
    Video video = new Video();
    video.setId(id);
    video.setFileName(TEST_VIDEO_FILENAME);
    video.setFilePath(bucketKey);
    video.setWidth(VIDEO_WIDTH);
    video.setHeight(VIDEO_HEIGHT);
    video.setDuration(VIDEO_DURATION);
    video.setFrameRate(VIDEO_FRAME_RATE);
    video.setSize(VIDEO_SIZE);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(AUDIO_CHANNELS);
    video.setAudioSampleRate(AUDIO_SAMPLE_RATE);
    video.setAudioCodec(AudioCodec.AAC);
    video.setCreatedAt(LocalDateTime.now());
    video.setOwner(createMockUser());
    return toJVideo(video);
  }

  private JAudio createMockJAudio(String id, String bucketKey) {
    Audio audio = new Audio();
    audio.setId(id);
    audio.setFileName(TEST_AUDIO_FILENAME);
    audio.setFilePath(bucketKey);
    audio.setDuration(AUDIO_DURATION);
    audio.setBitRate(AUDIO_BIT_RATE);
    audio.setSampleRate(AUDIO_SAMPLE_RATE);
    audio.setChannels(AUDIO_CHANNELS);
    audio.setCodec(AudioCodec.AAC);
    audio.setFormat(ContainerFormat.MP3);
    audio.setSize(AUDIO_SIZE);
    audio.setSizeType(SizeType.BYTES);
    audio.setFileType(FileType.AUDIO);
    audio.setCreatedAt(LocalDateTime.now());
    audio.setOwner(createMockUser());
    return toJAudio(audio);
  }

  private User createMockUser() {
    User user = new User();
    user.setId(randomUUID().toString());
    user.setEmail(VideoServiceIT.TEST_USER_EMAIL);
    return user;
  }

  private VideoCompressionJob createMockCompressedJob(
      String jobId,
      JVideo parentVideo,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt) {
    VideoCompressionJob job = new VideoCompressionJob();
    job.setId(jobId);
    job.setParent(parentVideo);
    job.setStatus(status);
    job.setCreatedAt(createdAt);
    job.setCompletedAt(completedAt);
    job.setAttemptCount(status == ProcessStatus.COMPLETED ? 1 : 0);

    if (status == ProcessStatus.COMPLETED) {
      JVideo compressedVideo = createMockJVideo(randomUUID().toString(), COMPRESSED_KEY);
      compressedVideo.setBucketKey(
          buildCompressedVideoUrl(COMPRESSED_VIDEO_PREFIX + jobId + ".mp4"));
      job.setCompressedVideo(compressedVideo);
    }

    return job;
  }
}
