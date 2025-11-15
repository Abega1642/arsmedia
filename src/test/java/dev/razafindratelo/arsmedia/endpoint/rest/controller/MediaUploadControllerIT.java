package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.*;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.service.UserService;
import dev.razafindratelo.arsmedia.service.media.VideoMetaDataExtractor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
class MediaUploadControllerIT extends FacadeIT {

  private static final String TEST_EMAIL = "rakoto@gmail.com";
  private static final String VIDEO_ENDPOINT = "/api/media/videos/upload";
  private static final String TEST_VIDEO_FILENAME = "test-video.webm";
  private static final String VIDEO_CONTENT_TYPE = "video/webm";
  private static final String FILE_PARAM = "file";
  private static final String EMAIL_PARAM = "userEmail";

  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;
  @Autowired private VideoRepository videoRepository;
  @Autowired private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    videoRepository.deleteAll();
    ensureTestUserExists();
  }

  @AfterEach
  void tearDown() {
    videoRepository.deleteAll();
  }

  @Test
  void should_upload_video_successfully() throws Exception {
    MockMultipartFile file = createTestVideoFile();

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file).param(EMAIL_PARAM, TEST_EMAIL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.file_name").value(startsWith("upload-")))
        .andExpect(jsonPath("$.owner.email").value(TEST_EMAIL));
    entityManager.clear();

    var videos = videoRepository.findAllWithOwner();
    assertThat(videos).hasSize(1);
    assertThat(videos.getFirst().getOwner().getEmail()).isEqualTo(TEST_EMAIL);
  }

  @Test
  void should_return_bad_request_for_invalid_email() throws Exception {
    MockMultipartFile file = createTestVideoFile();

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file).param(EMAIL_PARAM, "invalid-email"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_bad_request_when_file_is_missing() throws Exception {
    mvc.perform(multipart(VIDEO_ENDPOINT).param(EMAIL_PARAM, TEST_EMAIL))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_bad_request_when_email_is_missing() throws Exception {
    MockMultipartFile file = createTestVideoFile();

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file)).andExpect(status().isBadRequest());
  }

  @Test
  void should_return_forbidden_when_user_not_activated() throws Exception {
    userService.updateActivationStatusByEmail(TEST_EMAIL, false);
    MockMultipartFile file = createTestVideoFile();

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file).param(EMAIL_PARAM, TEST_EMAIL))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_return_not_found_when_user_does_not_exist() throws Exception {
    MockMultipartFile file = createTestVideoFile();
    String nonExistentEmail = "nonexistent@example.com";

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file).param(EMAIL_PARAM, nonExistentEmail))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_handle_multiple_video_uploads() throws Exception {
    MockMultipartFile file1 = createTestVideoFile();
    MockMultipartFile file2 =
        new MockMultipartFile(
            FILE_PARAM, "test-video-2.webm", VIDEO_CONTENT_TYPE, "video content 2".getBytes());

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file1).param(EMAIL_PARAM, TEST_EMAIL))
        .andExpect(status().isOk());
    entityManager.clear();

    mvc.perform(multipart(VIDEO_ENDPOINT).file(file2).param(EMAIL_PARAM, TEST_EMAIL))
        .andExpect(status().isOk());

    entityManager.clear();

    var videos = videoRepository.findAllWithOwner();
    assertThat(videos).hasSize(2);
    assertThat(videos).allMatch(video -> video.getOwner().getEmail().equals(TEST_EMAIL));
  }

  private MockMultipartFile createTestVideoFile() {
    return new MockMultipartFile(
        FILE_PARAM, TEST_VIDEO_FILENAME, VIDEO_CONTENT_TYPE, "video content".getBytes());
  }

  private void ensureTestUserExists() {
    try {
      userService.findByEmail(TEST_EMAIL);
      userService.updateActivationStatusByEmail(TEST_EMAIL, true);
    } catch (EntityNotFoundException e) {
      UserCreationRequest request =
          new UserCreationRequest(TEST_EMAIL, "1234", "johnDoe", UserRole.USER, "secure-password");
      userService.create(request);
      userService.updateActivationStatusByEmail(TEST_EMAIL, true);
    }
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public VideoMetaDataExtractor videoMetaDataExtractor() throws IOException {
      return new VideoMetaDataExtractor() {
        @Override
        public Video apply(File file) {
          Video video = new Video();
          video.setId(UUID.randomUUID().toString());
          video.setFileName(file.getName());
          video.setFileType(FileType.VIDEO);
          video.setSize(file.length());
          video.setSizeType(SizeType.BYTES);
          video.setCreatedAt(LocalDateTime.now());
          video.setDuration(120.0);
          video.setWidth(1920);
          video.setHeight(1080);
          video.setFrameRate(30.0);
          video.setCodec(VideoCodec.H264);
          video.setContainerFormat(ContainerFormat.WEBM);
          return video;
        }
      };
    }
  }
}
