package dev.razafindratelo.arsmedia.service;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.razafindratelo.arsmedia.exception.MediaUploadException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.service.media.VideoMetaDataExtractor;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@ExtendWith(MockitoExtension.class)
class MediaServiceIT {

  private static final String TEST_EMAIL = "rakoto@gmail.com";
  private static final String VIDEO_ID = "video-123";
  private static final String VIDEO_FILENAME = "test-video.webm";
  private static final Long VIDEO_DURATION = 120L;
  private static final String VIDEO_PREFIX = "videos/";
  private static final String FAILED_TO_UPLOAD_VIDEO_MESSAGE = "Failed to upload video";
  private static final String USER_ACCOUNT_IS_NOT_ACTIVATED_MESSAGE =
      "User account is not activated";
  @Mock private BucketComponent bucket;
  @Mock private VideoMetaDataExtractor videoExtractor;
  @Mock private VideoRepository videoRepository;
  @Mock private UserService userService;
  @Mock private MultipartFileConverter fileConverter;
  @InjectMocks private MediaService mediaService;

  @Test
  void should_upload_video_successfully() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var videoFile = mock(File.class);
    var activeUser = createActiveUser();
    var video = createTestVideo();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);
    when(fileConverter.convert(multipartFile)).thenReturn(videoFile);
    when(videoExtractor.apply(videoFile)).thenReturn(video);

    var result = mediaService.uploadVideo(multipartFile, TEST_EMAIL);

    assertThat(result).isNotNull();
    assertThat(result.getOwner()).isEqualTo(activeUser);
    assertThat(result.getFilePath()).isNotNull();
    assertThat(result.getFilePath()).startsWith(VIDEO_PREFIX);

    verify(userService).findByEmail(TEST_EMAIL);
    verify(fileConverter).convert(multipartFile);
    verify(videoExtractor).apply(videoFile);
    verify(bucket).upload(eq(videoFile), startsWith(VIDEO_PREFIX));
    verify(videoRepository).save(any());
  }

  @Test
  void should_set_correct_owner_and_file_path_on_video() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var videoFile = mock(File.class);
    var activeUser = createActiveUser();
    var video = createTestVideo();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);
    when(fileConverter.convert(multipartFile)).thenReturn(videoFile);
    when(videoExtractor.apply(videoFile)).thenReturn(video);

    var result = mediaService.uploadVideo(multipartFile, TEST_EMAIL);

    assertThat(result.getOwner()).isEqualTo(activeUser);
    assertThat(result.getFilePath()).matches("videos/[a-f0-9-]{36}");
  }

  @Test
  void should_throw_exception_when_user_not_activated() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var inactiveUser = createInactiveUser();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(inactiveUser);

    assertThatThrownBy(() -> mediaService.uploadVideo(multipartFile, TEST_EMAIL))
        .isInstanceOf(UserNotActivatedException.class)
        .hasMessageContaining(USER_ACCOUNT_IS_NOT_ACTIVATED_MESSAGE)
        .hasMessageContaining(TEST_EMAIL);

    verify(userService).findByEmail(TEST_EMAIL);
    verify(fileConverter, never()).convert(any());
    verify(bucket, never()).upload(any(), any());
    verify(videoRepository, never()).save(any());
  }

  @Test
  void should_throw_exception_when_file_conversion_fails() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var activeUser = createActiveUser();
    var conversionException = new IOException("File conversion failed");

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);
    when(fileConverter.convert(multipartFile)).thenThrow(conversionException);

    assertThatThrownBy(() -> mediaService.uploadVideo(multipartFile, TEST_EMAIL))
        .isInstanceOf(MediaUploadException.class)
        .hasMessageContaining(FAILED_TO_UPLOAD_VIDEO_MESSAGE);

    verify(bucket, never()).upload(any(), any());
    verify(videoRepository, never()).save(any());
  }

  @Test
  void should_throw_not_implemented_exception_for_audio_upload() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var activeUser = createActiveUser();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);

    assertThatThrownBy(() -> mediaService.uploadAudio(multipartFile, TEST_EMAIL))
        .isInstanceOf(NotImplementedException.class)
        .hasMessageContaining("Audio upload not yet implemented");

    verify(userService).findByEmail(TEST_EMAIL);
    verify(fileConverter, never()).convert(any());
  }

  @Test
  void should_throw_not_implemented_exception_for_image_upload() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var activeUser = createActiveUser();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);

    assertThatThrownBy(() -> mediaService.uploadImage(multipartFile, TEST_EMAIL))
        .isInstanceOf(NotImplementedException.class)
        .hasMessageContaining("Image upload not yet implemented");

    verify(userService).findByEmail(TEST_EMAIL);
    verify(fileConverter, never()).convert(any());
  }

  @Test
  void should_validate_user_before_audio_upload() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    User inactiveUser = createInactiveUser();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(inactiveUser);

    assertThatThrownBy(() -> mediaService.uploadAudio(multipartFile, TEST_EMAIL))
        .isInstanceOf(UserNotActivatedException.class)
        .hasMessageContaining(USER_ACCOUNT_IS_NOT_ACTIVATED_MESSAGE);
  }

  @Test
  void should_validate_user_before_image_upload() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    User inactiveUser = createInactiveUser();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(inactiveUser);

    assertThatThrownBy(() -> mediaService.uploadImage(multipartFile, TEST_EMAIL))
        .isInstanceOf(UserNotActivatedException.class)
        .hasMessageContaining(USER_ACCOUNT_IS_NOT_ACTIVATED_MESSAGE);
  }

  @Test
  void should_generate_unique_bucket_keys_for_multiple_uploads() throws IOException {
    var multipartFile1 = mock(MultipartFile.class);
    var multipartFile2 = mock(MultipartFile.class);
    var videoFile1 = mock(File.class);
    var videoFile2 = mock(File.class);
    var activeUser = createActiveUser();
    var video1 = createTestVideo();
    var video2 = createTestVideo();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);
    when(fileConverter.convert(multipartFile1)).thenReturn(videoFile1);
    when(fileConverter.convert(multipartFile2)).thenReturn(videoFile2);
    when(videoExtractor.apply(videoFile1)).thenReturn(video1);
    when(videoExtractor.apply(videoFile2)).thenReturn(video2);

    Video result1 = mediaService.uploadVideo(multipartFile1, TEST_EMAIL);
    Video result2 = mediaService.uploadVideo(multipartFile2, TEST_EMAIL);

    assertThat(result1.getFilePath()).isNotEqualTo(result2.getFilePath());
    assertThat(result1.getFilePath()).startsWith(VIDEO_PREFIX);
    assertThat(result2.getFilePath()).startsWith(VIDEO_PREFIX);
  }

  @Test
  void should_save_video_with_correct_mapping() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var videoFile = mock(File.class);
    var activeUser = createActiveUser();
    var video = createTestVideo();

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(activeUser);
    when(fileConverter.convert(multipartFile)).thenReturn(videoFile);
    when(videoExtractor.apply(videoFile)).thenReturn(video);

    mediaService.uploadVideo(multipartFile, TEST_EMAIL);

    ArgumentCaptor<JVideo> captor = ArgumentCaptor.forClass(JVideo.class);
    verify(videoRepository).save(captor.capture());

    var savedVideo = captor.getValue();
    assertThat(savedVideo).isNotNull();
  }

  private User createActiveUser() {
    User user = new User();
    user.setId(randomUUID().toString());
    user.setEmail(TEST_EMAIL);
    user.setActivated(true);
    return user;
  }

  private User createInactiveUser() {
    User user = new User();
    user.setId(randomUUID().toString());
    user.setEmail(TEST_EMAIL);
    user.setActivated(false);
    return user;
  }

  private Video createTestVideo() {
    Video video = new Video();
    video.setId(VIDEO_ID);
    video.setFileName(VIDEO_FILENAME);
    video.setDuration(VIDEO_DURATION);
    return video;
  }
}
