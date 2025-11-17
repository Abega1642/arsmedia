package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.AudioMapper.toAudio;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
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

import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.exception.AudioExtractionException;
import dev.razafindratelo.arsmedia.exception.DirectoryUploadException;
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
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.AudioRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.JAudio;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.service.UserService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
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
class AudioExtractionRequestedServiceIT {

  public static final String PREFIX = "audio_extracted_";
  public static final String TEST_USER = "test@example.com";
  public static final String TEST_BUCKET_KEY = "test_bucket_key";
  private static final String FFMPEG_PATH = "/usr/bin/ffmpeg";
  private static final String FFPROBE_PATH = "/usr/bin/ffprobe";

  private static final int ORIGINAL_VIDEO_SIZE = 10_000_000;
  private static final int EXTRACTED_AUDIO_SIZE = 2_000_000;
  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String VIDEO_KEY = "video_key";
  private static final String SUFFIX = ".mp3";
  private static final String TEST_VIDEO_MP_4 = "test_video.mp4";
  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();

  @TempDir File tempDir;
  private File interceptedAudioFile;
  @Mock private BucketComponent bucketComponent;
  @Mock private VideoRepository videoRepository;
  @Mock private AudioRepository audioRepository;
  @Mock private UserService userService;
  @Mock private AudioExtractionJobRepository audioExtractionJobRepository;
  private AudioExtractionRequestedService service;

  private static @NotNull AudioExtractionJob getSnapshot(AudioExtractionJob job) {
    AudioExtractionJob snapshot = new AudioExtractionJob();
    snapshot.setId(job.getId());
    snapshot.setParent(job.getParent());
    snapshot.setExtractedAudio(job.getExtractedAudio());
    snapshot.setCreatedAt(job.getCreatedAt());
    snapshot.setCompletedAt(job.getCompletedAt());
    snapshot.setStatus(job.getStatus());
    snapshot.setErrorMessage(job.getErrorMessage());
    snapshot.setAttemptCount(job.getAttemptCount());
    return snapshot;
  }

  @BeforeEach
  void setUp() throws IOException {
    service =
        new AudioExtractionRequestedService(
            FFMPEG_PATH,
            FFPROBE_PATH,
            bucketComponent,
            videoRepository,
            audioRepository,
            userService,
            audioExtractionJobRepository,
            tempFileCleaner);
  }

  @Test
  void should_successfully_extract_audio_from_video() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, VIDEO_KEY);

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        extractionJob);

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      verifySuccessfulAudioExtraction(videoId);
      verifyStatusUpdates(jobId, ProcessStatus.PROGRESSING, ProcessStatus.COMPLETED);
    }
  }

  @Test
  void should_extract_audio_with_stereo_channels() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, "stereo_video_key");

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithStereoAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        extractionJob);

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      ArgumentCaptor<JAudio> audioCaptor = ArgumentCaptor.forClass(JAudio.class);
      verify(audioRepository).save(audioCaptor.capture());

      Audio savedAudio = toAudio(audioCaptor.getValue());
      assertEquals(2, savedAudio.getChannels());
      assertEquals(48000, savedAudio.getSampleRate());
      assertEquals(AudioCodec.MP3, savedAudio.getCodec());
      assertEquals(ContainerFormat.MP3, savedAudio.getFormat());

      log.info("Audio extracted successfully with {} channels", savedAudio.getChannels());
    }
  }

  @Test
  void should_extract_audio_with_mono_channel() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, "mono_video_key");

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithMonoAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        extractionJob);

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      ArgumentCaptor<JAudio> audioCaptor = ArgumentCaptor.forClass(JAudio.class);
      verify(audioRepository).save(audioCaptor.capture());

      Audio savedAudio = toAudio(audioCaptor.getValue());
      assertEquals(1, savedAudio.getChannels());

      log.info("Mono audio extracted successfully");
    }
  }

  @Test
  void should_throw_exception_when_video_has_no_audio() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, "no_audio_key");

    JVideo videoWithoutAudio = createMockJVideoWithoutAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, videoWithoutAudio);

    when(audioExtractionJobRepository.findById(jobId)).thenReturn(Optional.of(extractionJob));
    when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithoutAudio));

    assertThrows(AudioExtractionException.class, () -> service.accept(event));

    ArgumentCaptor<AudioExtractionJob> captor = ArgumentCaptor.forClass(AudioExtractionJob.class);
    verify(audioExtractionJobRepository, atLeast(1)).save(captor.capture());

    boolean hasFailed =
        captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
    assertTrue(hasFailed, "Job should have been marked as FAILED");

    verify(audioRepository, never()).save(any());
    verify(bucketComponent, never()).upload(any(), anyString());

    log.info("Correctly rejected video without audio");
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, "key");

    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, new JVideo());

    when(audioExtractionJobRepository.findById(jobId)).thenReturn(Optional.of(extractionJob));
    when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

    assertThrows(AudioExtractionException.class, () -> service.accept(event));

    verify(audioExtractionJobRepository, atLeast(1)).save(any(AudioExtractionJob.class));
    verify(audioRepository, never()).save(any());
    verify(bucketComponent, never()).upload(any(), anyString());
  }

  @Test
  void should_handle_ffmpeg_execution_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, VIDEO_KEY);

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        extractionJob);

    try (var mockedExecutor =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, context) -> {
              FFmpegJob mockJob = mock(FFmpegJob.class);
              when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
              doThrow(new RuntimeException("FFmpeg audio extraction failed")).when(mockJob).run();
            })) {

      assertThrows(AudioExtractionException.class, () -> service.accept(event));

      ArgumentCaptor<AudioExtractionJob> captor = ArgumentCaptor.forClass(AudioExtractionJob.class);
      verify(audioExtractionJobRepository, atLeast(1)).save(captor.capture());

      Optional<AudioExtractionJob> failedJob =
          captor.getAllValues().stream()
              .filter(job -> job.getStatus() == ProcessStatus.FAILED)
              .findFirst();

      assertTrue(failedJob.isPresent(), "Job should have been marked as FAILED");
      assertNotNull(failedJob.get().getErrorMessage(), "Error message should be present");
      assertTrue(failedJob.get().getErrorMessage().contains("FFmpeg audio extraction failed"));
    }
  }

  @Test
  void should_handle_upload_failure() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, VIDEO_KEY);

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    when(audioExtractionJobRepository.findById(jobId)).thenReturn(Optional.of(extractionJob));
    when(videoRepository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(event.getOwner())).thenReturn(createMockUser(event.getOwner()));
    when(bucketComponent.download(event.getBucketKey())).thenReturn(originalFile);

    try (var mockedExecutor = setupFfmpegMock()) {
      doThrow(new DirectoryUploadException("Upload failed"))
          .when(bucketComponent)
          .upload(any(File.class), anyString());

      assertThrows(AudioExtractionException.class, () -> service.accept(event));

      verify(audioRepository, never()).save(any(JAudio.class));

      ArgumentCaptor<AudioExtractionJob> captor = ArgumentCaptor.forClass(AudioExtractionJob.class);
      verify(audioExtractionJobRepository, atLeast(1)).save(captor.capture());

      boolean hasFailed =
          captor.getAllValues().stream().anyMatch(job -> job.getStatus() == ProcessStatus.FAILED);
      assertTrue(hasFailed, "Job should have been marked as FAILED");
    }
  }

  @Test
  void should_track_retry_attempts() {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, "key");
    event.setAttemptNb(2);

    JVideo originalJVideo = createMockJVideoWithAudio(videoId);
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    when(audioExtractionJobRepository.findById(jobId)).thenReturn(Optional.of(extractionJob));
    when(videoRepository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(AudioExtractionException.class, () -> service.accept(event));

    ArgumentCaptor<AudioExtractionJob> captor = ArgumentCaptor.forClass(AudioExtractionJob.class);
    verify(audioExtractionJobRepository, atLeast(1)).save(captor.capture());

    Optional<AudioExtractionJob> failedJob =
        captor.getAllValues().stream()
            .filter(job -> job.getStatus() == ProcessStatus.FAILED)
            .findFirst();

    assertTrue(failedJob.isPresent());
    assertEquals(2, failedJob.get().getAttemptCount(), "Attempt count should be 2");
  }

  @Test
  void should_generate_correct_audio_filename() throws IOException {
    var videoId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var event = createAudioExtractionEvent(videoId, jobId, VIDEO_KEY);

    File originalFile = createMockVideoFile();
    JVideo originalJVideo = createMockJVideoWithAudio(videoId);
    originalJVideo.setFileName("my_awesome_video.mp4");
    AudioExtractionJob extractionJob = createMockExtractionJob(jobId, originalJVideo);

    setupMocks(
        originalJVideo,
        createMockUser(OWNER_EMAIL),
        event.getBucketKey(),
        originalFile,
        extractionJob);

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      ArgumentCaptor<JAudio> audioCaptor = ArgumentCaptor.forClass(JAudio.class);
      verify(audioRepository).save(audioCaptor.capture());

      Audio savedAudio = toAudio(audioCaptor.getValue());
      assertTrue(savedAudio.getFileName().startsWith(PREFIX));
      assertTrue(savedAudio.getFileName().endsWith(SUFFIX));
      assertTrue(savedAudio.getFileName().contains("my_awesome_video"));

      log.info("Audio filename generated correctly: {}", savedAudio.getFileName());
    }
  }

  private AudioExtractionRequested createAudioExtractionEvent(
      String videoId, String jobId, String bucketKey) {
    return AudioExtractionRequested.builder()
        .videoId(videoId)
        .jobId(jobId)
        .bucketKey(bucketKey)
        .owner(AudioExtractionRequestedServiceIT.OWNER_EMAIL)
        .build();
  }

  private void setupMocks(
      JVideo originalJVideo,
      User owner,
      String bucketKey,
      File originalFile,
      AudioExtractionJob extractionJob) {

    lenient()
        .when(audioExtractionJobRepository.findById(extractionJob.getId()))
        .thenAnswer(
            invocation -> {
              AudioExtractionJob copy = getSnapshot(extractionJob);
              return Optional.of(copy);
            });

    lenient().when(videoRepository.findById(any())).thenReturn(Optional.of(originalJVideo));
    lenient().when(userService.findByEmail(any())).thenReturn(owner);
    lenient().when(bucketComponent.download(bucketKey)).thenReturn(originalFile);

    lenient()
        .when(audioExtractionJobRepository.save(any(AudioExtractionJob.class)))
        .thenAnswer(
            invocation -> {
              AudioExtractionJob job = invocation.getArgument(0);
              extractionJob.setStatus(job.getStatus());
              extractionJob.setExtractedAudio(job.getExtractedAudio());
              extractionJob.setCompletedAt(job.getCompletedAt());
              extractionJob.setErrorMessage(job.getErrorMessage());
              extractionJob.setAttemptCount(job.getAttemptCount());

              return getSnapshot(job);
            });

    lenient()
        .doAnswer(
            invocation -> {
              File file = invocation.getArgument(0);
              if (file != null && file.exists()) {
                interceptedAudioFile = file;
                log.info(
                    "Upload called with file: {}, size: {} bytes", file.getName(), file.length());
              }
              return null;
            })
        .when(bucketComponent)
        .upload(any(File.class), anyString());
  }

  private MockedConstruction<FFmpegExecutor> setupFfmpegMock() {
    return mockConstruction(
        FFmpegExecutor.class,
        (mock, context) -> {
          FFmpegJob mockJob = mock(FFmpegJob.class);
          when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);

          doAnswer(
                  invocation -> {
                    File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
                    File[] audioFiles =
                        systemTempDir.listFiles(
                            (dir, name) -> name.startsWith(PREFIX) && name.endsWith(SUFFIX));

                    if (audioFiles != null && audioFiles.length > 0) {
                      File actualOutputFile =
                          Arrays.stream(audioFiles)
                              .max(Comparator.comparingLong(File::lastModified))
                              .orElseThrow(
                                  () -> new RuntimeException("Could not find audio output file"));

                      log.info(
                          "Writing mock audio data to: {}", actualOutputFile.getAbsolutePath());

                      byte[] audioData = new byte[EXTRACTED_AUDIO_SIZE];
                      new Random().nextBytes(audioData);
                      Files.write(actualOutputFile.toPath(), audioData);

                      interceptedAudioFile = actualOutputFile;

                      log.info(
                          "Successfully wrote {} bytes to audio file", actualOutputFile.length());
                    } else {
                      throw new RuntimeException(
                          "No extracted_ temp file was created by the service");
                    }

                    return null;
                  })
              .when(mockJob)
              .run();
        });
  }

  private void verifySuccessfulAudioExtraction(String videoId) {
    verify(bucketComponent).download(anyString());

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(any(File.class), keyCaptor.capture());

    ArgumentCaptor<JAudio> audioCaptor = ArgumentCaptor.forClass(JAudio.class);
    verify(audioRepository).save(audioCaptor.capture());

    Audio savedAudio = toAudio(audioCaptor.getValue());
    var actualAudioSize = savedAudio.getSize();

    assertTrue(actualAudioSize > 0, "Audio file should not be empty");
    assertTrue(
        actualAudioSize < ORIGINAL_VIDEO_SIZE, "Audio file should be smaller than original video");

    log.info(
        "Audio extraction verified: {}MB audio extracted", actualAudioSize / (1024.0 * 1024.0));

    String uploadedKey = keyCaptor.getValue();
    assertTrue(uploadedKey.startsWith(PREFIX + videoId));
    assertTrue(uploadedKey.endsWith(SUFFIX));

    assertTrue(savedAudio.getFileName().startsWith(PREFIX));
    assertTrue(savedAudio.getFileName().endsWith(SUFFIX));
    assertEquals(AudioCodec.MP3, savedAudio.getCodec());
    assertEquals(ContainerFormat.MP3, savedAudio.getFormat());
    assertEquals(FileType.AUDIO, savedAudio.getFileType());
  }

  private void verifyStatusUpdates(String jobId, ProcessStatus... expectedStatuses) {
    ArgumentCaptor<AudioExtractionJob> captor = ArgumentCaptor.forClass(AudioExtractionJob.class);
    verify(audioExtractionJobRepository, atLeast(expectedStatuses.length)).save(captor.capture());

    List<ProcessStatus> actualStatuses =
        captor.getAllValues().stream().map(AudioExtractionJob::getStatus).toList();

    for (ProcessStatus expectedStatus : expectedStatuses) {
      assertTrue(
          actualStatuses.contains(expectedStatus),
          "Expected status " + expectedStatus + " not found in: " + actualStatuses);
    }
  }

  private File createMockVideoFile() throws IOException {
    File file = new File(tempDir, TEST_VIDEO_MP_4);
    Files.write(
        file.toPath(),
        new byte[(int) (long) AudioExtractionRequestedServiceIT.ORIGINAL_VIDEO_SIZE]);
    return file;
  }

  private JVideo createMockJVideoWithAudio(String videoId) {
    return createMockJVideo(videoId, 2, 48000);
  }

  private JVideo createMockJVideoWithStereoAudio(String videoId) {
    return createMockJVideo(videoId, 2, 48000);
  }

  private JVideo createMockJVideoWithMonoAudio(String videoId) {
    return createMockJVideo(videoId, 1, 44100);
  }

  private JVideo createMockJVideo(String videoId, int audioChannels, int sampleRate) {
    Video video = new Video();
    video.setId(videoId);
    video.setFileName(TEST_VIDEO_MP_4);
    video.setWidth(1920);
    video.setHeight(1080);
    video.setDuration(120.0);
    video.setFrameRate(30.0);
    video.setSize(ORIGINAL_VIDEO_SIZE);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(audioChannels);
    video.setAudioSampleRate(sampleRate);
    video.setAudioCodec(AudioCodec.AAC);
    video.setCreatedAt(LocalDateTime.now());
    video.setOwner(createMockUser(TEST_USER));
    video.setFilePath(TEST_BUCKET_KEY);
    return toJVideo(video);
  }

  private JVideo createMockJVideoWithoutAudio(String videoId) {
    Video video = new Video();
    video.setId(videoId);
    video.setFileName("video_no_audio.mp4");
    video.setWidth(1920);
    video.setHeight(1080);
    video.setDuration(120.0);
    video.setFrameRate(30.0);
    video.setSize(ORIGINAL_VIDEO_SIZE);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(0);
    video.setAudioSampleRate(0);
    video.setAudioCodec(AudioCodec.NONE);
    video.setCreatedAt(LocalDateTime.now());
    video.setOwner(createMockUser(TEST_USER));
    video.setFilePath(TEST_BUCKET_KEY);
    return toJVideo(video);
  }

  private User createMockUser(String email) {
    User user = new User();
    user.setId(randomUUID().toString());
    user.setEmail(email);
    return user;
  }

  private AudioExtractionJob createMockExtractionJob(String jobId, JVideo parentVideo) {
    AudioExtractionJob job = new AudioExtractionJob();
    job.setId(jobId);
    job.setParent(parentVideo);
    job.setStatus(ProcessStatus.PENDING);
    job.setCreatedAt(LocalDateTime.now());
    job.setAttemptCount(0);
    return job;
  }
}
