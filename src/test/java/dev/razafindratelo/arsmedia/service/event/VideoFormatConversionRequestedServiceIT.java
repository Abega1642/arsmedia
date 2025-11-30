package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.event.model.VideoFormatConversionRequested;
import dev.razafindratelo.arsmedia.exception.DirectoryUploadException;
import dev.razafindratelo.arsmedia.exception.VideoFormatConversionException;
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
import dev.razafindratelo.arsmedia.repository.VideoFormatConversionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.repository.model.job.VideoFormatConversionJob;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.util.BitRateCalculator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.jetbrains.annotations.NotNull;
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
class VideoFormatConversionRequestedServiceIT {

  private static final int ORIGINAL_FILE_SIZE = 10_000_000;
  private static final int CONVERTED_FILE_SIZE = 9_500_000;
  private static final String PREFIX = "converted_";
  private static final String MP4_SUFFIX = ".mp4";
  private static final String MKV_EXTENSION = ".mkv";
  private static final String VIDEO_KEY = "video_key";
  private static final String OWNER_EMAIL = "owner@example.com";
  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();
  private final BitRateCalculator bitRateCalculator = new BitRateCalculator();

  @TempDir File tempDir;

  @Mock private FFmpeg fFmpeg;
  @Mock private FFprobe fFprobe;
  @Mock private BucketComponent bucketComponent;
  @Mock private VideoRepository repository;
  @Mock private UserService userService;
  @Mock private VideoFormatConversionJobRepository videoFormatConversionJobRepository;

  private VideoFormatConversionRequestedService subject;
  private File interceptedConvertedFile;

  private static @NotNull VideoFormatConversionJob getVideoFormatConversionJob(
      VideoFormatConversionJob job) {
    return VideoFormatConversionJob.builder()
        .id(job.getId())
        .parent(job.getParent())
        .convertedVideo(job.getConvertedVideo())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .status(job.getStatus())
        .errorMessage(job.getErrorMessage())
        .attemptCount(job.getAttemptCount())
        .build();
  }

  @BeforeEach
  void setUp() {
    subject =
        new VideoFormatConversionRequestedService(
            fFmpeg,
            fFprobe,
            bucketComponent,
            repository,
            userService,
            videoFormatConversionJobRepository,
            tempFileCleaner,
            bitRateCalculator);
  }

  @Test
  void should_successfully_convert_mp4_to_mkv() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(MKV_EXTENSION)) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MKV, VideoCodec.H264, AudioCodec.AAC);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_mp4_to_webm() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.WEBM);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(".webm")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.WEBM, VideoCodec.VP9, AudioCodec.OPUS);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_mov_to_mp4() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MP4);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mov");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MOV, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(MP4_SUFFIX)) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MP4, VideoCodec.H264, AudioCodec.AAC);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_avi_to_mp4() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MP4);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".avi");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.AVI, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(MP4_SUFFIX)) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MP4, VideoCodec.H264, AudioCodec.AAC);
    }
  }

  @Test
  void should_successfully_convert_to_flv() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.FLV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(".flv")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.FLV, VideoCodec.FLV, AudioCodec.MP3);
    }
  }

  @Test
  void should_successfully_convert_to_3gp() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.THREEGP);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(".3gp")) {
      subject.accept(event);

      verifySuccessfulConversion(
          videoId, ContainerFormat.THREEGP, VideoCodec.H263, AudioCodec.AMR_NB);
    }
  }

  @Test
  void should_handle_video_without_audio() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_no_audio_key", ContainerFormat.MKV);

    File originalFile = createMockVideoFile(8_000_000, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideoWithoutAudio(videoId);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(MKV_EXTENSION)) {
      subject.accept(event);

      verifyVideoWithoutAudio();
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_throw_exception_when_converting_video_to_audio_format() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MP3);

    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    ArgumentCaptor<VideoFormatConversionJob> captor =
        ArgumentCaptor.forClass(VideoFormatConversionJob.class);
    verify(videoFormatConversionJobRepository, atLeast(1)).save(captor.capture());

    boolean hasFailed =
        captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
    assertTrue(hasFailed, "Job should have been marked as FAILED");

    verifyNoSuccessfulConversionOperations();
  }

  @Test
  void should_throw_exception_when_source_and_target_formats_are_same() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MP4);

    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    verifyNoSuccessfulConversionOperations();
  }

  @Test
  void should_throw_exception_when_target_format_is_unknown() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.UNKNOWN);

    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    verifyNoSuccessfulConversionOperations();
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "key", ContainerFormat.MKV);

    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, new JVideo());

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.empty());

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    verify(videoFormatConversionJobRepository, atLeast(1))
        .save(any(VideoFormatConversionJob.class));
    verifyNoSuccessfulConversionOperations();
  }

  @Test
  void should_throw_exception_when_download_fails() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "failing_key", ContainerFormat.MKV);

    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    ArgumentCaptor<VideoFormatConversionJob> captor =
        ArgumentCaptor.forClass(VideoFormatConversionJob.class);
    verify(videoFormatConversionJobRepository, atLeast(1)).save(captor.capture());

    boolean hasFailed =
        captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
    assertTrue(hasFailed, "Job should have been marked as FAILED");

    verifyNoSuccessfulConversionOperations();
  }

  @Test
  void should_handle_ffmpeg_execution_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, _) -> {
              FFmpegJob mockJob = mock(FFmpegJob.class);
              when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
              doThrow(new RuntimeException("FFmpeg conversion failed")).when(mockJob).run();
            })) {

      assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

      ArgumentCaptor<VideoFormatConversionJob> captor =
          ArgumentCaptor.forClass(VideoFormatConversionJob.class);
      verify(videoFormatConversionJobRepository, atLeast(1)).save(captor.capture());

      Optional<VideoFormatConversionJob> failedJob =
          captor.getAllValues().stream()
              .filter(job -> job.getStatus() == ProcessStatus.FAILED)
              .findFirst();

      assertTrue(failedJob.isPresent(), "Job should have been marked as FAILED");
      assertNotNull(failedJob.get().getErrorMessage(), "Error message should be present");
      assertTrue(failedJob.get().getErrorMessage().contains("FFmpeg conversion failed"));
    }
  }

  @Test
  void should_handle_upload_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(event.getOwner())).thenReturn(createMockUser(event.getOwner()));
    when(bucketComponent.download(event.getBucketKey())).thenReturn(originalFile);

    try (var _ = setupFfmpegMock(MKV_EXTENSION)) {
      doThrow(new DirectoryUploadException("Upload failed"))
          .when(bucketComponent)
          .upload(any(File.class), anyString());

      assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

      verify(repository, never()).save(any(JVideo.class));

      ArgumentCaptor<VideoFormatConversionJob> captor =
          ArgumentCaptor.forClass(VideoFormatConversionJob.class);
      verify(videoFormatConversionJobRepository, atLeast(1)).save(captor.capture());

      boolean hasFailed =
          captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
      assertTrue(hasFailed, "Job should have been marked as FAILED");
    }
  }

  @Test
  void should_track_retry_attempts() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "key", ContainerFormat.MKV);
    event.setAttemptNb(3);

    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(VideoFormatConversionException.class, () -> subject.accept(event));

    ArgumentCaptor<VideoFormatConversionJob> captor =
        ArgumentCaptor.forClass(VideoFormatConversionJob.class);
    verify(videoFormatConversionJobRepository, atLeast(1)).save(captor.capture());

    Optional<VideoFormatConversionJob> failedJob =
        captor.getAllValues().stream()
            .filter(job -> job.getStatus() == ProcessStatus.FAILED)
            .findFirst();

    assertTrue(failedJob.isPresent());
    assertEquals(3, failedJob.get().getAttemptCount(), "Attempt count should be 3");
  }

  @Test
  void should_preserve_video_properties_during_conversion() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, VIDEO_KEY, ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, MP4_SUFFIX);
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var _ = setupFfmpegMock(MKV_EXTENSION)) {
      subject.accept(event);

      ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
      verify(repository).save(jVideoCaptor.capture());

      Video savedVideo = toVideo(jVideoCaptor.getValue());
      assertEquals(1_920, savedVideo.getWidth());
      assertEquals(1_080, savedVideo.getHeight());
      assertEquals(30.0, savedVideo.getFrameRate());
      assertEquals(120.0, savedVideo.getDuration());
      assertEquals(2, savedVideo.getAudioChannels());
      assertEquals(48_000, savedVideo.getAudioSampleRate());
    }
  }

  private VideoFormatConversionRequested createConversionEvent(
      String videoId, String jobId, String bucketKey, ContainerFormat targetFormat) {
    return VideoFormatConversionRequested.builder()
        .videoId(videoId)
        .jobId(jobId)
        .bucketKey(bucketKey)
        .owner(OWNER_EMAIL)
        .targetFormat(targetFormat)
        .build();
  }

  private void setupMocks(
      JVideo originalJVideo,
      User owner,
      String bucketKey,
      File originalFile,
      VideoFormatConversionJob conversionJob) {

    lenient()
        .when(videoFormatConversionJobRepository.findById(conversionJob.getId()))
        .thenAnswer(
            _ -> {
              VideoFormatConversionJob copy = getVideoFormatConversionJob(conversionJob);
              return Optional.of(copy);
            });

    lenient().when(repository.findById(any())).thenReturn(Optional.of(originalJVideo));
    lenient().when(userService.findByEmail(any())).thenReturn(owner);
    lenient().when(bucketComponent.download(bucketKey)).thenReturn(originalFile);

    lenient()
        .when(videoFormatConversionJobRepository.save(any(VideoFormatConversionJob.class)))
        .thenAnswer(
            invocation -> {
              VideoFormatConversionJob job = invocation.getArgument(0);
              conversionJob.setStatus(job.getStatus());
              conversionJob.setConvertedVideo(job.getConvertedVideo());
              conversionJob.setCompletedAt(job.getCompletedAt());
              conversionJob.setErrorMessage(job.getErrorMessage());
              conversionJob.setAttemptCount(job.getAttemptCount());

              return getVideoFormatConversionJob(job);
            });

    lenient()
        .doAnswer(
            invocation -> {
              File file = invocation.getArgument(0);
              if (file != null && file.exists()) {
                interceptedConvertedFile = file;
                log.info(
                    "Upload called with file: {}, size: {} bytes", file.getName(), file.length());
              }
              return null;
            })
        .when(bucketComponent)
        .upload(any(File.class), anyString());
  }

  private MockedConstruction<FFmpegExecutor> setupFfmpegMock(String extension) {
    return FFmpegMockHelper.setupFfmpegMock(
        PREFIX, extension, CONVERTED_FILE_SIZE, file -> interceptedConvertedFile = file);
  }

  private void verifySuccessfulConversion(
      String videoId,
      ContainerFormat expectedFormat,
      VideoCodec expectedVideoCodec,
      AudioCodec expectedAudioCodec) {

    verify(bucketComponent).download(anyString());

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(any(File.class), keyCaptor.capture());

    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    var actualConvertedSize = savedVideo.getSize();

    assertTrue(actualConvertedSize > 0, "Converted file should not be empty");

    log.info(
        "Conversion verified: format={}, size={}MB",
        expectedFormat,
        actualConvertedSize / (1024.0 * 1024.0));

    String uploadedKey = keyCaptor.getValue();
    assertTrue(uploadedKey.startsWith(PREFIX + videoId));

    assertTrue(savedVideo.getFileName().startsWith(PREFIX));
    assertEquals(expectedFormat, savedVideo.getContainerFormat());
    assertEquals(expectedVideoCodec, savedVideo.getCodec());
    assertEquals(expectedAudioCodec, savedVideo.getAudioCodec());
  }

  private void verifyVideoWithoutAudio() {
    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    assertEquals(ContainerFormat.MKV, savedVideo.getContainerFormat());
    assertEquals(AudioCodec.NONE, savedVideo.getAudioCodec());
    assertEquals(0, savedVideo.getAudioChannels());
    assertEquals(0, savedVideo.getAudioSampleRate());
  }

  private void verifyStatusUpdates(ProcessStatus... expectedStatuses) {
    ArgumentCaptor<VideoFormatConversionJob> captor =
        ArgumentCaptor.forClass(VideoFormatConversionJob.class);
    verify(videoFormatConversionJobRepository, atLeast(expectedStatuses.length))
        .save(captor.capture());

    List<ProcessStatus> actualStatuses =
        captor.getAllValues().stream().map(VideoFormatConversionJob::getStatus).toList();

    for (ProcessStatus expectedStatus : expectedStatuses) {
      assertTrue(
          actualStatuses.contains(expectedStatus),
          "Expected status " + expectedStatus + " not found in: " + actualStatuses);
    }
  }

  private void verifyNoSuccessfulConversionOperations() {
    verify(bucketComponent, never()).upload(any(), anyString());
  }

  private File createMockVideoFile(long size, String extension) throws IOException {
    var file = new File(tempDir, "test_video" + extension);
    Files.write(file.toPath(), new byte[(int) size]);
    return file;
  }

  private JVideo createMockJVideo(String videoId, ContainerFormat format, boolean hasAudio) {
    var video =
        Video.builder()
            .id(videoId)
            .fileName("original" + "." + format.name().toLowerCase())
            .width(1920)
            .height(1080)
            .frameRate(30.0)
            .duration(120.0)
            .size(ORIGINAL_FILE_SIZE)
            .sizeType(SizeType.BYTES)
            .fileType(FileType.VIDEO)
            .codec(getCodecForFormat(format))
            .containerFormat(format)
            .audioChannels(hasAudio ? 2 : 0)
            .audioSampleRate(hasAudio ? 48000 : 0)
            .audioCodec(hasAudio ? AudioCodec.AAC : AudioCodec.NONE)
            .owner(createMockUser("test@example.com"))
            .build();

    return toJVideo(video);
  }

  private JVideo createMockJVideoWithoutAudio(String videoId) {
    return createMockJVideo(videoId, ContainerFormat.MP4, false);
  }

  private VideoFormatConversionJob createMockConversionJob(String jobId, JVideo parent) {
    return VideoFormatConversionJob.builder()
        .id(jobId)
        .parent(parent)
        .createdAt(now())
        .status(ProcessStatus.PENDING)
        .attemptCount(0)
        .build();
  }

  private User createMockUser(String email) {
    return User.builder().id(randomUUID().toString()).email(email).build();
  }

  private VideoCodec getCodecForFormat(ContainerFormat format) {
    return switch (format) {
      case AVI -> VideoCodec.MPEG4;
      case FLV -> VideoCodec.FLV;
      case WEBM -> VideoCodec.VP9;
      case THREEGP -> VideoCodec.H263;
      default -> VideoCodec.H264;
    };
  }
}
