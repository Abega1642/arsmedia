package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.COMPLETED;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.PROGRESSING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.exception.DirectoryUploadException;
import dev.razafindratelo.arsmedia.exception.VideoProcessingException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.*;
import dev.razafindratelo.arsmedia.repository.CompressedVideoRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.service.UserService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
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
class VideoCompressionRequestedServiceIT {
  private static final int ORIGINAL_FILE_SIZE = 10_000_000;
  private static final int COMPRESSED_FILE_SIZE = 5_000_000;
  private static final double MINIMUM_COMPRESSION_RATIO = 0.3;

  @TempDir File tempDir;

  @Mock private BucketComponent bucketComponent;

  @Mock private VideoRepository repository;

  @Mock private UserService userService;

  @Mock private CompressedVideoRepository compressedVideoRepository;

  private VideoCompressionRequestedService service;
  private File interceptedCompressedFile;

  @BeforeEach
  void setUp() throws IOException {
    service =
        new VideoCompressionRequestedService(
            bucketComponent, repository, userService, compressedVideoRepository);
  }

  @Test
  void should_successfully_compress_video_with_custom_resolution() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event =
        createCompressionEvent(videoId, "original_video_key", "owner@example.com", 23, 1280, 720);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    JCompressedVideo savedCompressedVideo = createMockCompressedVideo(PROGRESSING);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        savedCompressedVideo);

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      verifySuccessfulCompressionWithSizeReduction(videoId, 1280, 720, true);
      verifyCompressionStatusUpdate(savedCompressedVideo.getId(), COMPLETED);
    }
  }

  @Test
  void should_handle_video_without_audio() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event =
        createCompressionEvent(videoId, "video_no_audio_key", "owner@example.com", 23, null, null);

    File originalFile = createMockVideoFile(8_000_000);
    JVideo originalJVideo = createMockJVideoWithoutAudio(videoId);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        createMockCompressedVideo(PROGRESSING));

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);
      verifyVideoWithoutAudio();
    }
  }

  @Test
  void should_adjust_odd_dimensions_to_even() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event =
        createCompressionEvent(videoId, "odd_dimensions_key", "owner@example.com", 23, 1281, 721);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        createMockCompressedVideo(PROGRESSING));

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);
      verifySuccessfulCompressionWithSizeReduction(videoId, 1280, 720, true);
    }
  }

  @Test
  void should_use_original_dimensions_when_not_specified() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event =
        createCompressionEvent(
            videoId, "original_dimensions_key", "owner@example.com", 23, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        createMockCompressedVideo(PROGRESSING));

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);
      verifySuccessfulCompressionWithSizeReduction(videoId, 1920, 1080, true);
    }
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "key", "owner@example.com", 23, null, null);

    when(repository.findById(videoId)).thenReturn(Optional.empty());

    assertThrows(VideoProcessingException.class, () -> service.accept(event));
    verifyNoCompressionOperations();
  }

  @Test
  void should_throw_exception_when_download_fails() {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "failing_key", "owner@example.com", 23, null, null);

    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(event.getBucketKey()))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThrows(VideoProcessingException.class, () -> service.accept(event));
    verifyNoCompressionOperations();
  }

  @Test
  void should_use_default_frame_rate_when_invalid() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "video_key", "owner@example.com", 23, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideoWithInvalidFrameRate(videoId);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        createMockCompressedVideo(PROGRESSING));

    try (var mockedExecutor = setupFfmpegMock()) {
      service.accept(event);

      ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
      verify(repository).save(jVideoCaptor.capture());

      Video savedVideo = toVideo(jVideoCaptor.getValue());
      assertEquals(30.0, savedVideo.getFrameRate());
    }
  }

  @Test
  void should_handle_ffmpeg_execution_failure() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "video_key", "owner@example.com", 23, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);

    setupMocks(
        originalJVideo,
        createMockUser("owner@example.com"),
        event.getBucketKey(),
        originalFile,
        createMockCompressedVideo(PROGRESSING));

    try (var mockedExecutor =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, context) -> {
              FFmpegJob mockJob = mock(FFmpegJob.class);
              when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
              doThrow(new RuntimeException("FFmpeg processing failed")).when(mockJob).run();
            })) {
      assertThrows(VideoProcessingException.class, () -> service.accept(event));
      verify(compressedVideoRepository, never())
          .updateCompressedVideoStatus(eq(COMPLETED), anyString());
    }
  }

  @Test
  void should_handle_upload_failure() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "video_key", "owner@example.com", 23, null, null);

    File originalFile = createMockVideoFile(ORIGINAL_FILE_SIZE);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);

    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(event.getOwner())).thenReturn(createMockUser(event.getOwner()));
    when(bucketComponent.download(event.getBucketKey())).thenReturn(originalFile);

    try (var mockedExecutor = setupFfmpegMock()) {
      doThrow(new DirectoryUploadException("Upload failed"))
          .when(bucketComponent)
          .upload(any(File.class), anyString());

      assertThrows(VideoProcessingException.class, () -> service.accept(event));

      verify(repository, never()).save(any(JVideo.class));
      verify(compressedVideoRepository, never()).save(any(JCompressedVideo.class));
      verify(compressedVideoRepository, never())
          .updateCompressedVideoStatus(eq(COMPLETED), anyString());
    }
  }

  private VideoCompressionRequested createCompressionEvent(
      String videoId,
      String bucketKey,
      String ownerEmail,
      Integer crf,
      Integer targetWidth,
      Integer targetHeight) {

    var optionsBuilder = CompressionOptions.builder().crf(crf);
    if (targetWidth != null) optionsBuilder.targetWidth(targetWidth);
    if (targetHeight != null) optionsBuilder.targetHeight(targetHeight);

    return VideoCompressionRequested.builder()
        .videoId(videoId)
        .bucketKey(bucketKey)
        .owner(ownerEmail)
        .compressionOptions(optionsBuilder.build())
        .build();
  }

  private void setupMocks(
      JVideo originalJVideo,
      User owner,
      String bucketKey,
      File originalFile,
      JCompressedVideo compressedVideo) {
    lenient().when(repository.findById(any())).thenReturn(Optional.of(originalJVideo));
    lenient().when(userService.findByEmail(any())).thenReturn(owner);
    lenient().when(bucketComponent.download(bucketKey)).thenReturn(originalFile);
    lenient()
        .when(compressedVideoRepository.save(any(JCompressedVideo.class)))
        .thenReturn(compressedVideo);

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
    return mockConstruction(
        FFmpegExecutor.class,
        (mock, context) -> {
          FFmpegJob mockJob = mock(FFmpegJob.class);
          when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);

          doAnswer(
                  invocation -> {
                    File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
                    File[] compressedFiles =
                        systemTempDir.listFiles(
                            (dir, name) -> name.startsWith("compressed_") && name.endsWith(".mp4"));

                    if (compressedFiles != null && compressedFiles.length > 0) {
                      File actualOutputFile =
                          Arrays.stream(compressedFiles)
                              .max(Comparator.comparingLong(File::lastModified))
                              .orElseThrow(
                                  () ->
                                      new RuntimeException(
                                          "Could not find compressed output file"));

                      log.info(
                          "Writing mock compressed data to: {}",
                          actualOutputFile.getAbsolutePath());

                      byte[] compressedData = new byte[COMPRESSED_FILE_SIZE];
                      new Random().nextBytes(compressedData);
                      Files.write(actualOutputFile.toPath(), compressedData);

                      interceptedCompressedFile = actualOutputFile;

                      log.info(
                          "Successfully wrote {} bytes to compressed file",
                          actualOutputFile.length());
                    } else {
                      throw new RuntimeException(
                          "No compressed_ temp file was created by the service");
                    }

                    return null;
                  })
              .when(mockJob)
              .run();
        });
  }

  private void verifySuccessfulCompressionWithSizeReduction(
      String videoId, int expectedWidth, int expectedHeight, boolean hasAudio) {
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
    assertTrue(uploadedKey.startsWith("compressed_" + videoId));

    assertTrue(savedVideo.getFileName().startsWith("compressed_"));
    assertEquals(expectedWidth, savedVideo.getWidth());
    assertEquals(expectedHeight, savedVideo.getHeight());
    assertEquals(VideoCodec.H264, savedVideo.getCodec());
    assertEquals(ContainerFormat.MP4, savedVideo.getContainerFormat());

    if (hasAudio) {
      assertEquals(AudioCodec.AAC, savedVideo.getAudioCodec());
    }
  }

  private void verifyVideoWithoutAudio() {
    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    assertEquals(AudioCodec.NONE, savedVideo.getAudioCodec());
    assertEquals(0, savedVideo.getAudioChannels());
    assertEquals(0, savedVideo.getAudioSampleRate());
  }

  private void verifyCompressionStatusUpdate(
      String compressedVideoId, ProcessStatus expectedStatus) {
    verify(compressedVideoRepository)
        .updateCompressedVideoStatus(eq(expectedStatus), eq(compressedVideoId));
  }

  private void verifyNoCompressionOperations() {
    verify(compressedVideoRepository, never()).save(any());
    verify(compressedVideoRepository, never()).updateCompressedVideoStatus(any(), anyString());
    verify(bucketComponent, never()).upload(any(), anyString());
  }

  private File createMockVideoFile(long size) throws IOException {
    File file = new File(tempDir, "test_video.mp4");
    Files.write(file.toPath(), new byte[(int) size]);
    return file;
  }

  private JVideo createMockJVideo(
      String videoId, int width, int height, double frameRate, boolean hasAudio) {
    Video video = new Video();
    video.setId(videoId);
    video.setFileName("original.mp4");
    video.setWidth(width);
    video.setHeight(height);
    video.setFrameRate(frameRate);
    video.setDuration(120.0);
    video.setSize(ORIGINAL_FILE_SIZE);
    video.setSizeType(SizeType.MB);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);

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
    return createMockJVideo(videoId, 1920, 1080, 30.0, false);
  }

  private JVideo createMockJVideoWithInvalidFrameRate(String videoId) {
    Video video = new Video();
    video.setId(videoId);
    video.setFileName("original.mp4");
    video.setWidth(1920);
    video.setHeight(1080);
    video.setFrameRate(-1.0);
    video.setDuration(120.0);
    video.setSize(ORIGINAL_FILE_SIZE);
    video.setSizeType(SizeType.BYTES);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(2);
    video.setAudioSampleRate(48000);
    video.setOwner(createMockUser("test@example.com"));
    video.setAudioCodec(AudioCodec.AAC);
    return toJVideo(video);
  }

  private JCompressedVideo createMockCompressedVideo(ProcessStatus status) {
    return new JCompressedVideo(
        UUID.randomUUID().toString(), new JVideo(), LocalDateTime.now(), status);
  }

  private User createMockUser(String email) {
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setEmail(email);
    return user;
  }
}
