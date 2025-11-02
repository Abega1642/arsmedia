package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.validation.annotation.Validated;

@Validated
@Disabled
public class MediaUploadControllerIT extends FacadeIT {
  private final String TEST_EMAIL = "rakoto@gmail.com";
  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;

  public static MockMultipartFile convertFileToMultipartFile(@NotNull File file)
      throws IOException {
    if (!file.exists()) {
      throw new IllegalArgumentException("The provided file does not exist.");
    }
    try (FileInputStream input = new FileInputStream(file)) {
      return new MockMultipartFile("file", file.getName(), "application/octet-stream", input);
    }
  }

  @BeforeEach
  void setUp() {
    var testUser =
        new UserCreationRequest(TEST_EMAIL, "1234", "johnDoe", UserRole.USER, "random-password");
    userService.create(testUser);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  @Test
  void should_upload_the_file_video_successfully() throws Exception {
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectFile = convertFileToMultipartFile(new File(resource.toURI()));

    mvc.perform(
            MockMvcRequestBuilders.multipart("/api/media/videos/upload")
                .file(subjectFile)
                .param("userEmail", TEST_EMAIL))
        .andExpect(status().isOk());
  }
}
