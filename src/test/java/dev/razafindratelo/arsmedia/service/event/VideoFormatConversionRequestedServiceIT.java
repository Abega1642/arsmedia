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
import static org.mockito.Mockito.doAnswer;
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
import dev.razafindratelo.arsmedia.service.BitRateCalculator;
import dev.razafindratelo.arsmedia.service.UserService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
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
class VideoFormatConversionRequestedServiceIT {
  private static final int ORIGINAL_FILE_SIZE = 10_000_000;
  private static final int CONVERTED_FILE_SIZE = 9_500_000;
  private static final String PREFIX = "converted_";
  private static final String FFMPEG_PATH = "/usr/bin/ffmpeg";
  private static final String FFPROBE_PATH = "/usr/bin/ffprobe";
  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();
  private final BitRateCalculator bitRateCalculator = new BitRateCalculator();

  @TempDir File tempDir;

  @Mock private BucketComponent bucketComponent;
  @Mock private VideoRepository repository;
  @Mock private UserService userService;
  @Mock private VideoFormatConversionJobRepository videoFormatConversionJobRepository;

  private VideoFormatConversionRequestedService subject;
  private File interceptedConvertedFile;

  private static @NotNull VideoFormatConversionJob getVideoFormatConversionJob(
      VideoFormatConversionJob job) {
    VideoFormatConversionJob snapshot = new VideoFormatConversionJob();
    snapshot.setId(job.getId());
    snapshot.setParent(job.getParent());
    snapshot.setConvertedVideo(job.getConvertedVideo());
    snapshot.setCreatedAt(job.getCreatedAt());
    snapshot.setCompletedAt(job.getCompletedAt());
    snapshot.setStatus(job.getStatus());
    snapshot.setErrorMessage(job.getErrorMessage());
    snapshot.setAttemptCount(job.getAttemptCount());
    return snapshot;
  }

  @BeforeEach
  void setUp() throws IOException {
    subject =
        new VideoFormatConversionRequestedService(
            FFMPEG_PATH,
            FFPROBE_PATH,
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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".mkv")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MKV, VideoCodec.H264, AudioCodec.AAC);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_mp4_to_webm() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.WEBM);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".webm")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.WEBM, VideoCodec.VP9, AudioCodec.OPUS);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_mov_to_mp4() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MP4);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mov");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MOV, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".mp4")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MP4, VideoCodec.H264, AudioCodec.AAC);
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_successfully_convert_avi_to_mp4() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MP4);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".avi");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.AVI, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".mp4")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.MP4, VideoCodec.H264, AudioCodec.AAC);
    }
  }

  @Test
  void should_successfully_convert_to_flv() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.FLV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".flv")) {
      subject.accept(event);

      verifySuccessfulConversion(videoId, ContainerFormat.FLV, VideoCodec.FLV, AudioCodec.MP3);
    }
  }

  @Test
  void should_successfully_convert_to_3gp() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.THREEGP);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".3gp")) {
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

    File originalFile = createMockVideoFile(8_000_000, ".mp4");
    JVideo originalJVideo = createMockJVideoWithoutAudio(videoId);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".mkv")) {
      subject.accept(event);

      verifyVideoWithoutAudio();
      verifyStatusUpdates(ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_throw_exception_when_converting_video_to_audio_format() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MP3);

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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MP4);

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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.UNKNOWN);

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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, context) -> {
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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    when(videoFormatConversionJobRepository.findById(jobId)).thenReturn(Optional.of(conversionJob));
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(event.getOwner())).thenReturn(createMockUser(event.getOwner()));
    when(bucketComponent.download(event.getBucketKey())).thenReturn(originalFile);

    try (var mockedExecutor = setupFfmpegMock(".mkv")) {
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
    var event = createConversionEvent(videoId, jobId, "video_key", ContainerFormat.MKV);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE, ".mp4");
    JVideo originalJVideo = createMockJVideo(videoId, ContainerFormat.MP4, true);
    VideoFormatConversionJob conversionJob = createMockConversionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        conversionJob);

    try (var mockedExecutor = setupFfmpegMock(".mkv")) {
      subject.accept(event);

      ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
      verify(repository).save(jVideoCaptor.capture());

      Video savedVideo = toVideo(jVideoCaptor.getValue());
      assertEquals(1920, savedVideo.getWidth());
      assertEquals(1080, savedVideo.getHeight());
      assertEquals(30.0, savedVideo.getFrameRate());
      assertEquals(120.0, savedVideo.getDuration());
      assertEquals(2, savedVideo.getAudioChannels());
      assertEquals(48000, savedVideo.getAudioSampleRate());
    }
  }

  private VideoFormatConversionRequested createConversionEvent(
      String videoId, String jobId, String bucketKey, ContainerFormat targetFormat) {

    return VideoFormatConversionRequested.builder()
        .videoId(videoId)
        .jobId(jobId)
        .bucketKey(bucketKey)
        .owner("owner@example.com")
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
            invocation -> {
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
    return mockConstruction(
        FFmpegExecutor.class,
        (mock, context) -> {
          FFmpegJob mockJob = mock(FFmpegJob.class);
          when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);

          doAnswer(
                  invocation -> {
                    File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
                    File[] convertedFiles =
                        systemTempDir.listFiles(
                            (dir, name) -> name.startsWith(PREFIX) && name.endsWith(extension));

                    if (convertedFiles != null && convertedFiles.length > 0) {
                      File actualOutputFile =
                          Arrays.stream(convertedFiles)
                              .max(Comparator.comparingLong(File::lastModified))
                              .orElseThrow(
                                  () ->
                                      new RuntimeException("Could not find converted output file"));

                      log.info(
                          "Writing mock converted data to: {}", actualOutputFile.getAbsolutePath());

                      byte[] convertedData = new byte[CONVERTED_FILE_SIZE];
                      new Random().nextBytes(convertedData);
                      Files.write(actualOutputFile.toPath(), convertedData);

                      interceptedConvertedFile = actualOutputFile;

                      log.info(
                          "Successfully wrote {} bytes to converted file",
                          actualOutputFile.length());
                    } else {
                      throw new RuntimeException(
                          "No converted_ temp file was created by the service");
                    }

                    return null;
                  })
              .when(mockJob)
              .run();
        });
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
    File file = new File(tempDir, "test_video" + extension);
    Files.write(file.toPath(), new byte[(int) size]);
    return file;
  }

  private JVideo createMockJVideo(String videoId, ContainerFormat format, boolean hasAudio) {
    Video video = new Video();
    video.setId(videoId);
    video.setFileName("original" + "." + format.name().toLowerCase());
    video.setWidth(1920);
    video.setHeight(1080);
    video.setFrameRate(30.0);
    video.setDuration(120.0);
    video.setSize(ORIGINAL_FILE_SIZE);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(getCodecForFormat(format));
    video.setContainerFormat(format);

    if (hasAudio) {
      video.setAudioChannels(2);
      video.setAudioSampleRate(48000);
      video.setAudioCodec(AudioCodec.AAC);
    } else {
      video.setAudioChannels(0);
      video.setAudioSampleRate(0);
      video.setAudioCodec(AudioCodec.NONE);
    }

    video.setOwner(createMockUser("test@example.com"));
    return toJVideo(video);
  }

  private JVideo createMockJVideoWithoutAudio(String videoId) {
    return createMockJVideo(videoId, ContainerFormat.MP4, false);
  }

  private VideoFormatConversionJob createMockConversionJob(String jobId, JVideo parent) {
    VideoFormatConversionJob job = new VideoFormatConversionJob();
    job.setId(jobId);
    job.setParent(parent);
    job.setCreatedAt(now());
    job.setStatus(ProcessStatus.PENDING);
    job.setAttemptCount(0);
    return job;
  }

  private User createMockUser(String email) {
    User user = new User();
    user.setId(randomUUID().toString());
    user.setEmail(email);
    return user;
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
