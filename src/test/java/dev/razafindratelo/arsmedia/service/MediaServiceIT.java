package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
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
  private final String TEST_EMAIL = "john.doe@gmail.com";
  @Autowired private MediaService subject;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private VideoRepository videoRepository;

  @BeforeEach
  void setUp() {
    var testUser =
        new UserCreationRequest(TEST_EMAIL, "1234", "johnDoe", UserRole.USER, "random-password");
    userService.create(testUser);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteByEmail(TEST_EMAIL);
  }

  @Test
  void should_upload_video_successfully() throws URISyntaxException {
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectVideo = new File(resource.toURI());
    var actual = subject.uploadVideo(subjectVideo, TEST_EMAIL);

    log.info(actual.toString());

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getFilePath());

    videoRepository.deleteById(actual.getId());
  }

  @Test
  void should_not_allow_upload() throws URISyntaxException {
    userService.updateActivationStatusByEmail(TEST_EMAIL, false);
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectVideo = new File(resource.toURI());
    assertThrows(
        UserNotActivatedException.class, () -> subject.uploadVideo(subjectVideo, TEST_EMAIL));
  }
}
