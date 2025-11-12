package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toJVideo;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.COMPLETED;
import static dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.PROGRESSING;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
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
class VideoCompressionRequestedServiceIT {
  @TempDir File tempDir;
  @Mock private BucketComponent bucketComponent;
  @Mock private VideoRepository repository;
  @Mock private UserService userService;
  @Mock private CompressedVideoRepository compressedVideoRepository;

  private VideoCompressionRequestedService service;

  @BeforeEach
  void setUp() throws IOException {
    var ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
    var ffprobe = new FFprobe("/usr/bin/ffprobe");

    service =
        new VideoCompressionRequestedService(
            bucketComponent, repository, userService, compressedVideoRepository);
  }

  @Test
  void should_successfully_compress_video_with_custom_resolution() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "original_video_key";
    var ownerEmail = "owner@example.com";

    VideoCompressionRequested event =
        createCompressionEvent(videoId, bucketKey, ownerEmail, 23, 1280, 720);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    User owner = createMockUser(ownerEmail);

    JCompressedVideo savedCompressedVideo = createMockCompressedVideo(PROGRESSING);

    setupMocks(originalJVideo, owner, bucketKey, originalFile, savedCompressedVideo);

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(5_000_000)) {

      service.accept(event);

      verifySuccessfulCompression(videoId, 1280, 720, true);
      verifyCompressionStatusUpdate(savedCompressedVideo.getId(), COMPLETED);
    }
  }

  @Test
  void should_handle_video_without_audio() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "video_no_audio_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, null, null);

    File originalFile = createMockVideoFile("original_no_audio.mp4", 8_000_000);
    JVideo originalJVideo = createMockJVideoWithoutAudio(videoId);
    User owner = createMockUser(ownerEmail);

    setupMocks(
        originalJVideo, owner, bucketKey, originalFile, createMockCompressedVideo(PROGRESSING));

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(4_000_000)) {

      service.accept(event);

      verifyVideoWithoutAudio();
    }
  }

  @Test
  void should_adjust_odd_dimensions_to_even() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "odd_dimensions_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, 1281, 721);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    User owner = createMockUser(ownerEmail);

    setupMocks(
        originalJVideo, owner, bucketKey, originalFile, createMockCompressedVideo(PROGRESSING));

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(5_000_000)) {

      service.accept(event);

      verifySuccessfulCompression(videoId, 1280, 720, true);
    }
  }

  @Test
  void should_use_original_dimensions_when_not_specified() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "original_dimensions_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, null, null);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    User owner = createMockUser(ownerEmail);

    setupMocks(
        originalJVideo, owner, bucketKey, originalFile, createMockCompressedVideo(PROGRESSING));

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(5_000_000)) {

      service.accept(event);

      verifySuccessfulCompression(videoId, 1920, 1080, true);
    }
  }

  @Test
  void should_throw_exception_when_video_not_found() {
    var videoId = UUID.randomUUID().toString();
    var event = createCompressionEvent(videoId, "key", "owner@example.com", 23, null, null);

    when(repository.findById(videoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.accept(event))
        .isInstanceOf(VideoProcessingException.class)
        .hasMessageContaining("Video compression processing failed")
        .hasCauseInstanceOf(EntityNotFoundException.class);

    verifyNoCompressionOperations();
  }

  @Test
  void should_throw_exception_when_download_fails() {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "failing_key";

    var event = createCompressionEvent(videoId, bucketKey, "owner@example.com", 23, null, null);

    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(bucketComponent.download(bucketKey))
        .thenThrow(new DirectoryUploadException("Download failed"));

    assertThatThrownBy(() -> service.accept(event))
        .isInstanceOf(VideoProcessingException.class)
        .hasMessageContaining("Video compression processing failed");

    verifyNoCompressionOperations();
  }

  @Test
  void should_use_default_frame_rate_when_invalid() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "video_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, null, null);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideoWithInvalidFrameRate(videoId);
    User owner = createMockUser(ownerEmail);

    setupMocks(
        originalJVideo, owner, bucketKey, originalFile, createMockCompressedVideo(PROGRESSING));

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(5_000_000)) {

      service.accept(event);

      ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
      verify(repository).save(jVideoCaptor.capture());

      Video savedVideo = toVideo(jVideoCaptor.getValue());
      assertThat(savedVideo.getFrameRate()).isEqualTo(30.0);
    }
  }

  @Test
  void should_handle_ffmpeg_execution_failure() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "video_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, null, null);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    User owner = createMockUser(ownerEmail);

    setupMocks(
        originalJVideo, owner, bucketKey, originalFile, createMockCompressedVideo(PROGRESSING));

    try (MockedConstruction<FFmpegExecutor> mockedExecutor =
        mockConstruction(
            FFmpegExecutor.class,
            (mock, context) -> {
              FFmpegJob mockJob = mock(FFmpegJob.class);
              when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
              doThrow(new RuntimeException("FFmpeg processing failed")).when(mockJob).run();
            })) {

      assertThatThrownBy(() -> service.accept(event))
          .isInstanceOf(VideoProcessingException.class)
          .hasMessageContaining("Video compression processing failed");

      verify(compressedVideoRepository, never())
          .updateCompressedVideoStatus(eq(COMPLETED), anyString());
    }
  }

  @Test
  void should_handle_upload_failure() throws IOException {
    var videoId = UUID.randomUUID().toString();
    var bucketKey = "video_key";
    var ownerEmail = "owner@example.com";

    var event = createCompressionEvent(videoId, bucketKey, ownerEmail, 23, null, null);

    File originalFile = createMockVideoFile("original.mp4", 10_000_000);
    JVideo originalJVideo = createMockJVideo(videoId, 1920, 1080, 30.0, true);
    User owner = createMockUser(ownerEmail);

    when(repository.findById(videoId)).thenReturn(Optional.of(originalJVideo));
    when(userService.findByEmail(ownerEmail)).thenReturn(owner);
    when(bucketComponent.download(bucketKey)).thenReturn(originalFile);

    try (MockedConstruction<FFmpegExecutor> mockedExecutor = setupFfmpegMock(5_000_000)) {

      doThrow(new DirectoryUploadException("Upload failed"))
          .when(bucketComponent)
          .upload(any(File.class), anyString());

      assertThatThrownBy(() -> service.accept(event))
          .isInstanceOf(VideoProcessingException.class)
          .hasMessageContaining("Video compression processing failed");

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

    CompressionOptions.CompressionOptionsBuilder optionsBuilder =
        CompressionOptions.builder().crf(crf);

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
  }

  private MockedConstruction<FFmpegExecutor> setupFfmpegMock(int compressedSize) {
    return mockConstruction(
        FFmpegExecutor.class,
        (mock, context) -> {
          FFmpegJob mockJob = mock(FFmpegJob.class);
          when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);
          doAnswer(
                  invocation -> {
                    File tempFile = File.createTempFile("compressed_", ".mp4", tempDir);
                    Files.write(tempFile.toPath(), new byte[compressedSize]);
                    return null;
                  })
              .when(mockJob)
              .run();
        });
  }

  private void verifySuccessfulCompression(
      String videoId, int expectedWidth, int expectedHeight, boolean hasAudio) {

    verify(bucketComponent).download(anyString());

    ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(fileCaptor.capture(), keyCaptor.capture());

    String uploadedKey = keyCaptor.getValue();
    assertThat(uploadedKey).startsWith("compressed_" + videoId);

    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    assertThat(savedVideo.getFileName()).startsWith("compressed_");
    assertThat(savedVideo.getWidth()).isEqualTo(expectedWidth);
    assertThat(savedVideo.getHeight()).isEqualTo(expectedHeight);
    assertThat(savedVideo.getCodec()).isEqualTo(VideoCodec.H264);
    assertThat(savedVideo.getContainerFormat()).isEqualTo(ContainerFormat.MP4);

    if (hasAudio) {
      assertThat(savedVideo.getAudioCodec()).isEqualTo(AudioCodec.AAC);
    }
  }

  private void verifyVideoWithoutAudio() {
    ArgumentCaptor<JVideo> jVideoCaptor = ArgumentCaptor.forClass(JVideo.class);
    verify(repository).save(jVideoCaptor.capture());

    Video savedVideo = toVideo(jVideoCaptor.getValue());
    assertThat(savedVideo.getAudioCodec()).isEqualTo(AudioCodec.NONE);
    assertThat(savedVideo.getAudioChannels()).isEqualTo(0);
    assertThat(savedVideo.getAudioSampleRate()).isEqualTo(0);
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

  private File createMockVideoFile(String name, long size) throws IOException {
    File file = new File(tempDir, name);
    byte[] content = new byte[(int) size];
    Files.write(file.toPath(), content);
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
    video.setSize(10_000_000L);
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

    video.setOwner(createMockUser("mocked@user.com"));
    return toJVideo(video);
  }

  private JVideo createMockJVideo(String videoId) {
    return createMockJVideo(videoId, 1920, 1080, 30.0, true);
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
    video.setSize(10_000_000L);
    video.setSizeType(SizeType.MB);
    video.setFileType(FileType.VIDEO);
    video.setCodec(VideoCodec.H264);
    video.setContainerFormat(ContainerFormat.MP4);
    video.setAudioChannels(2);
    video.setAudioSampleRate(48000);
    video.setOwner(createMockUser("mocked@gmail.com"));
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
