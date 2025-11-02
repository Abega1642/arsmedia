package dev.razafindratelo.arsmedia.service;

import static com.jayway.jsonpath.JsonPath.read;
import static dev.razafindratelo.arsmedia.endpoint.rest.controller.MediaUploadControllerIT.convertFileToMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Disabled
class VideoServiceIT extends FacadeIT {

  private final String TEST_EMAIL = "jane.doe@gmail.com";
  @Autowired private MockMvc mvc;
  @Autowired private VideoService videoService;
  @Autowired private UserService userService;

  @BeforeEach
  void setUp() {
    var testUser =
        new UserCreationRequest(TEST_EMAIL, "1234", "johnDoe", UserRole.USER, "random-password");
    userService.create(testUser);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  @Test
  void should_process_compression_with_success() throws Exception {
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectFile = convertFileToMultipartFile(new File(resource.toURI()));

    MvcResult result =
        mvc.perform(MockMvcRequestBuilders.multipart("/api/media/videos/upload").file(subjectFile))
            .andExpect(status().isOk())
            .andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    String bucketKey = read(jsonResponse, "$.file_path");

    videoService.requestCompression(TEST_EMAIL, bucketKey);
  }
}
