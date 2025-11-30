package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.exception.DirectoryUploadException;
import dev.razafindratelo.arsmedia.exception.VideoProcessingException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.file.TempFileCleaner;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.repository.model.job.VideoCompressionJob;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Slf4j
@Getter
@Setter
class VideoCompressionRequestedServiceIT {
  private static final int ORIGINAL_FILE_SIZE = 10_000_000;
  private static final int COMPRESSED_FILE_SIZE = 5_000_000;
  private static final double MINIMUM_COMPRESSION_RATIO = 0.3;
  private static final String PREFIX = "compressed_";
  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String VIDEO_KEY = "video_key";

  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();
  private final BitRateCalculator bitRateCalculator = new BitRateCalculator();

  @TempDir File tempDir;

  @Mock private FFprobe fFprobe;
  @Mock private FFmpeg ffmpeg;
  @Mock private BucketComponent bucketComponent;
  @Mock private VideoRepository repository;
  @Mock private UserService userService;
  @Mock private VideoCompressionJobRepository videoCompressionJobRepository;

  private VideoCompressionRequestedService subject;
  private File interceptedCompressedFile;

  @BeforeEach
  void setUp() {
    subject =
        new VideoCompressionRequestedService(
            ffmpeg,
            fFprobe,
            bucketComponent,
            repository,
            userService,
            videoCompressionJobRepository,
            tempFileCleaner,
            bitRateCalculator);
  }

  @Test
  void should_successfully_compress_video_with_custom_resolution() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "original_video_key", 1280, 720);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ = setupFfmpegMock()) {
      subject.accept(event);

      verifySuccessfulCompressionWithSizeReduction(videoId, 1280, 720);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_handle_video_without_audio() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "video_no_audio_key", null, null);

    File originalFile = createMockVideoFile(8_000_000);
    JVideo originalJVideo = createMockJVideoWithoutAudio(videoId);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ = setupFfmpegMock()) {
      subject.accept(event);
      verifyVideoWithoutAudio();
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_adjust_odd_dimensions_to_even() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "odd_dimensions_key", 1281, 721);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ = setupFfmpegMock()) {
      subject.accept(event);
      verifySuccessfulCompressionWithSizeReduction(videoId, 1280, 720);
    }
  }

  @Test
  void should_use_original_dimensions_when_not_specified() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "original_dimensions_key", null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ = setupFfmpegMock()) {
      subject.accept(event);
      verifySuccessfulCompressionWithSizeReduction(videoId, 1920, 1080);
    }
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "key", null, null);

    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, new JVideo());

    when(videoCompressionJobRepository.findById(jobId)).thenReturn(Optional.of(compressionJob));
    when(repository.findById(videoId)).thenReturn(Optional.empty());

    assertThrows(VideoProcessingException.class, () -> subject.accept(event));

    verify(videoCompressionJobRepository, atLeast(1)).save(any(VideoCompressionJob.class));
    verifyNoSuccessfulCompressionOperations();
  }

  @Test
  void should_throw_exception_when_download_fails() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "failing_key", null, null);

    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    when(videoCompressionJobRepository.findById(jobId)).thenReturn(Optional.of(compressionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(VideoProcessingException.class, () -> subject.accept(event));

    ArgumentCaptor<VideoCompressionJob> captor = ArgumentCaptor.forClass(VideoCompressionJob.class);
    verify(videoCompressionJobRepository, atLeast(1)).save(captor.capture());

    boolean hasFailed =
        captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
    assertTrue(hasFailed, "Job should have been marked as FAILED");

    verifyNoSuccessfulCompressionOperations();
  }

  @Test
  void should_use_default_frame_rate_when_invalid() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, VIDEO_KEY, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideoWithInvalidFrameRate(videoId);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ = setupFfmpegMock()) {
      subject.accept(event);

      ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
      verify(repository).save(jVideoCaptor.capture());

      Video savedVideo = toVideo(jVideoCaptor.getValue());
      assertEquals(30.0, savedVideo.getFrameRate());
    }
  }

  @Test
  void should_handle_ffmpeg_execution_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, VIDEO_KEY, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        compressionJob);

    try (var _ =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, _) -> {
              FFmpegJob mockJob = mock(FFmpegJob.class);
              when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
              doThrow(new RuntimeException("FFmpeg processing failed")).when(mockJob).run();
            })) {

      assertThrows(VideoProcessingException.class, () -> subject.accept(event));

      ArgumentCaptor<VideoCompressionJob> captor =
          ArgumentCaptor.forClass(VideoCompressionJob.class);
      verify(videoCompressionJobRepository, atLeast(1)).save(captor.capture());

      Optional<VideoCompressionJob> failedJob =
          captor.getAllValues().stream()
              .filter(job -> job.getStatus() == ProcessStatus.FAILED)
              .findFirst();

      assertTrue(failedJob.isPresent(), "Job should have been marked as FAILED");
      assertNotNull(failedJob.get().getErrorMessage(), "Error message should be present");
      assertTrue(failedJob.get().getErrorMessage().contains("FFmpeg processing failed"));
    }
  }

  @Test
  void should_handle_upload_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, VIDEO_KEY, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    when(videoCompressionJobRepository.findById(jobId)).thenReturn(Optional.of(compressionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(event.getOwner())).thenReturn(createMockUser(event.getOwner()));
    when(bucketComponent.download(event.getBucketKey())).thenReturn(originalFile);

    try (var _ = setupFfmpegMock()) {
      doThrow(new DirectoryUploadException("Upload failed"))
          .when(bucketComponent)
          .upload(any(File.class), anyString());

      assertThrows(VideoProcessingException.class, () -> subject.accept(event));

      verify(repository, never()).save(any(JVideo.class));

      ArgumentCaptor<VideoCompressionJob> captor =
          ArgumentCaptor.forClass(VideoCompressionJob.class);
      verify(videoCompressionJobRepository, atLeast(1)).save(captor.capture());

      boolean hasFailed =
          captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
      assertTrue(hasFailed, "Job should have been marked as FAILED");
    }
  }

  @Test
  void should_track_retry_attempts() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createCompressionEvent(videoId, jobId, "key", null, null);
    event.setAttemptNb(3);

    JVideo originalJVideo = createMockJVideo(videoId, true);
    VideoCompressionJob compressionJob = createMockCompressedVideo(jobId, originalJVideo);

    when(videoCompressionJobRepository.findById(jobId)).thenReturn(Optional.of(compressionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(VideoProcessingException.class, () -> subject.accept(event));

    ArgumentCaptor<VideoCompressionJob> captor = ArgumentCaptor.forClass(VideoCompressionJob.class);
    verify(videoCompressionJobRepository, atLeast(1)).save(captor.capture());

    Optional<VideoCompressionJob> failedJob =
        captor.getAllValues().stream()
            .filter(job -> job.getStatus() == ProcessStatus.FAILED)
            .findFirst();

    assertTrue(failedJob.isPresent());
    assertEquals(3, failedJob.get().getAttemptCount(), "Attempt count should be 3");
  }

  private VideoCompressionRequested createCompressionEvent(
      String videoId, String jobId, String bucketKey, Integer targetWidth, Integer targetHeight) {

    var optionsBuilder = CompressionOptions.builder().crf(23);
    if (targetWidth != null) optionsBuilder.targetWidth(targetWidth);
    if (targetHeight != null) optionsBuilder.targetHeight(targetHeight);

    return VideoCompressionRequested.builder()
        .videoId(videoId)
        .jobId(jobId)
        .bucketKey(bucketKey)
        .owner(OWNER_EMAIL)
        .compressionOptions(optionsBuilder.build())
        .build();
  }

  private void setupMocks(
      JVideo originalJVideo,
      User owner,
      String bucketKey,
      File originalFile,
      VideoCompressionJob compressedVideo) {

    lenient()
        .when(videoCompressionJobRepository.findById(compressedVideo.getId()))
        .thenAnswer(
            _ -> {
              VideoCompressionJob copy =
                  VideoCompressionJob.builder()
                      .id(compressedVideo.getId())
                      .parent(compressedVideo.getParent())
                      .compressedVideo(compressedVideo.getCompressedVideo())
                      .createdAt(compressedVideo.getCreatedAt())
                      .completedAt(compressedVideo.getCompletedAt())
                      .status(compressedVideo.getStatus())
                      .errorMessage(compressedVideo.getErrorMessage())
                      .attemptCount(compressedVideo.getAttemptCount())
                      .build();
              return Optional.of(copy);
            });

    lenient().when(repository.findById(any())).thenReturn(Optional.of(originalJVideo));
    lenient().when(userService.findByEmail(any())).thenReturn(owner);
    lenient().when(bucketComponent.download(bucketKey)).thenReturn(originalFile);

    lenient()
        .when(videoCompressionJobRepository.save(any(VideoCompressionJob.class)))
        .thenAnswer(
            invocation -> {
              VideoCompressionJob job = invocation.getArgument(0);
              compressedVideo.setStatus(job.getStatus());
              compressedVideo.setCompressedVideo(job.getCompressedVideo());
              compressedVideo.setCompletedAt(job.getCompletedAt());
              compressedVideo.setErrorMessage(job.getErrorMessage());
              compressedVideo.setAttemptCount(job.getAttemptCount());

              return VideoCompressionJob.builder()
                  .id(job.getId())
                  .parent(job.getParent())
                  .compressedVideo(job.getCompressedVideo())
                  .createdAt(job.getCreatedAt())
                  .completedAt(job.getCompletedAt())
                  .status(job.getStatus())
                  .errorMessage(job.getErrorMessage())
                  .attemptCount(job.getAttemptCount())
                  .build();
            });

    lenient()
        .doAnswer(
            invocation -> {
              File file = invocation.getArgument(0);
              if (file != null && file.exists()) {
                interceptedCompressedFile = file;
                log.info(
                    "Upload called with file: {}, size: {} bytes", file.getName(), file.length());
              }
              return null;
            })
        .when(bucketComponent)
        .upload(any(File.class), anyString());
  }

  private MockedConstruction<FFmpegExecutor> setupFfmpegMock() {
    return FFmpegMockHelper.setupFfmpegMock(
        PREFIX, ".mp4", COMPRESSED_FILE_SIZE, file -> interceptedCompressedFile = file);
  }

  private void verifySuccessfulCompressionWithSizeReduction(
      String videoId, int expectedWidth, int expectedHeight) {
    verify(bucketComponent).download(anyString());

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(any(File.class), keyCaptor.capture());

    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    var actualCompressedSize = savedVideo.getSize();

    assertTrue(actualCompressedSize > 0, "Compressed file should not be empty");
    assertTrue(
        actualCompressedSize < ORIGINAL_FILE_SIZE,
        "Compressed file should be smaller than original. Original: "
            + ORIGINAL_FILE_SIZE
            + ", Compressed: "
            + actualCompressedSize);

    double sizeReductionRatio = (ORIGINAL_FILE_SIZE - actualCompressedSize) / ORIGINAL_FILE_SIZE;
    assertTrue(
        sizeReductionRatio >= MINIMUM_COMPRESSION_RATIO,
        String.format(
            "Should have at least %.0f%% size reduction. Actual: %.1f%%",
            MINIMUM_COMPRESSION_RATIO * 100, sizeReductionRatio * 100));

    log.info(
        "Compression verified: {}% reduction ({}MB -> {}MB)",
        sizeReductionRatio * 100,
        ORIGINAL_FILE_SIZE / (1024.0 * 1024.0),
        actualCompressedSize / (1024.0 * 1024.0));

    String uploadedKey = keyCaptor.getValue();
    assertTrue(uploadedKey.startsWith(PREFIX + videoId));

    assertTrue(savedVideo.getFileName().startsWith(PREFIX));
    assertEquals(expectedWidth, savedVideo.getWidth());
    assertEquals(expectedHeight, savedVideo.getHeight());
    assertEquals(VideoCodec.H264, savedVideo.getCodec());
    assertEquals(ContainerFormat.MP4, savedVideo.getContainerFormat());

    assertEquals(AudioCodec.AAC, savedVideo.getAudioCodec());
  }

  private void verifyVideoWithoutAudio() {
    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    assertEquals(AudioCodec.NONE, savedVideo.getAudioCodec());
    assertEquals(0, savedVideo.getAudioChannels());
    assertEquals(0, savedVideo.getAudioSampleRate());
  }

  private void verifyStatusUpdates(ProcessStatus... expectedStatuses) {
    ArgumentCaptor<VideoCompressionJob> captor = ArgumentCaptor.forClass(VideoCompressionJob.class);
    verify(videoCompressionJobRepository, atLeast(expectedStatuses.length)).save(captor.capture());

    List<ProcessStatus> actualStatuses =
        captor.getAllValues().stream().map(VideoCompressionJob::getStatus).toList();

    for (ProcessStatus expectedStatus : expectedStatuses) {
      assertTrue(
          actualStatuses.contains(expectedStatus),
          "Expected status " + expectedStatus + " not found in: " + actualStatuses);
    }
  }

  private void verifyNoSuccessfulCompressionOperations() {
    verify(bucketComponent, never()).upload(any(), anyString());
  }

  private File createMockVideoFile(long size) throws IOException {
    File file = new File(tempDir, "test_video.mp4");
    Files.write(file.toPath(), new byte[(int) size]);
    return file;
  }

  private JVideo createMockJVideo(String videoId, boolean hasAudio) {
    var video =
        Video.builder()
            .id(videoId)
            .fileName("original.mp4")
            .width(1920)
            .height(1080)
            .frameRate(30.0)
            .duration(120.0)
            .size(ORIGINAL_FILE_SIZE)
            .sizeType(SizeType.BYTES)
            .fileType(FileType.VIDEO)
            .codec(VideoCodec.H264)
            .containerFormat(ContainerFormat.MP4)
            .audioChannels(hasAudio ? 2 : 0)
            .audioSampleRate(hasAudio ? 48000 : 0)
            .audioCodec(hasAudio ? AudioCodec.AAC : AudioCodec.NONE)
            .owner(createMockUser(OWNER_EMAIL))
            .build();

    return toJVideo(video);
  }

  private JVideo createMockJVideoWithoutAudio(String videoId) {
    return createMockJVideo(videoId, false);
  }

  private JVideo createMockJVideoWithInvalidFrameRate(String videoId) {
    Video video =
        Video.builder()
            .id(videoId)
            .fileName("original.mp4")
            .width(1_920)
            .height(1_080)
            .frameRate(-1.0)
            .duration(120.0)
            .size(ORIGINAL_FILE_SIZE)
            .sizeType(SizeType.BYTES)
            .fileType(FileType.VIDEO)
            .codec(VideoCodec.H264)
            .containerFormat(ContainerFormat.MP4)
            .audioChannels(2)
            .audioSampleRate(48_000)
            .audioCodec(AudioCodec.AAC)
            .owner(createMockUser(OWNER_EMAIL))
            .build();

    return toJVideo(video);
  }

  private VideoCompressionJob createMockCompressedVideo(String jobId, JVideo parent) {
    return VideoCompressionJob.builder()
        .id(jobId)
        .parent(parent)
        .createdAt(LocalDateTime.now())
        .status(ProcessStatus.PENDING)
        .attemptCount(0)
        .build();
  }

  private User createMockUser(String email) {
    return User.builder().id(randomUUID().toString()).email(email).build();
  }
}
