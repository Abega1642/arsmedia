package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import java.io.File;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class MediaServiceIT extends FacadeIT {

  private static final String TEST_EMAIL = "john.doe@gmail.com";
  private static final String TEST_PHONE = "1234";
  private static final String TEST_USERNAME = "johnDoe";
  private static final String TEST_PASSWORD = "random-password";

  private static final String TEST_VIDEO_PATH = "/videos/test-video-one.webm";

  private static final String LOG_UPLOAD_SUCCESS = "Video uploaded successfully: {}";

  @Autowired private MediaService subject;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private VideoRepository videoRepository;

  @BeforeEach
  void setUp() {
    createAndActivateTestUser();
  }

  @AfterEach
  void tearDown() {
    cleanupTestData();
  }

  @Test
  void should_upload_video_successfully() throws URISyntaxException {
    File videoFile = getTestVideoFile();

    var uploadedVideo = subject.uploadVideo(videoFile, TEST_EMAIL);

    assertVideoUploadedSuccessfully(uploadedVideo);
    log.info(LOG_UPLOAD_SUCCESS, uploadedVideo);

    cleanupUploadedVideo(uploadedVideo.getId());
  }

  @Test
  void should_not_allow_upload() throws URISyntaxException {
    deactivateTestUser();
    File videoFile = getTestVideoFile();

    assertThrows(UserNotActivatedException.class, () -> subject.uploadVideo(videoFile, TEST_EMAIL));
  }

  private void createAndActivateTestUser() {
    var testUser =
        new UserCreationRequest(
            TEST_EMAIL, TEST_PHONE, TEST_USERNAME, UserRole.USER, TEST_PASSWORD);
    userService.create(testUser);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  private void deactivateTestUser() {
    userService.updateActivationStatusByEmail(TEST_EMAIL, false);
  }

  private void cleanupTestData() {
    userRepository.deleteByEmail(TEST_EMAIL);
  }

  private void cleanupUploadedVideo(String videoId) {
    videoRepository.deleteById(videoId);
  }

  private File getTestVideoFile() throws URISyntaxException {
    var resource = getClass().getResource(TEST_VIDEO_PATH);
    assertNotNull(resource, "Test video resource not found at: " + TEST_VIDEO_PATH);
    return new File(resource.toURI());
  }

  private void assertVideoUploadedSuccessfully(Video uploadedVideo) {
    assertNotNull(uploadedVideo, "Uploaded video should not be null");
    assertNotNull(uploadedVideo.getId(), "Video ID should not be null");
    assertNotNull(uploadedVideo.getFilePath(), "Video file path should not be null");
  }
}
