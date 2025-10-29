package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.validation.annotation.Validated;

@Validated
class MediaUploadControllerIT extends FacadeIT {
  @Autowired private MockMvc mvc;

  public static MockMultipartFile convertFileToMultipartFile(@NotNull File file)
      throws IOException {
    if (!file.exists()) {
      throw new IllegalArgumentException("The provided file does not exist.");
    }
    try (FileInputStream input = new FileInputStream(file)) {
      return new MockMultipartFile("file", file.getName(), "application/octet-stream", input);
    }
  }

  @Test
  void should_upload_the_file_video_successfully() throws Exception {
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectFile = convertFileToMultipartFile(new File(resource.toURI()));

    mvc.perform(MockMvcRequestBuilders.multipart("/api/media/videos/upload").file(subjectFile))
        .andExpect(status().isOk());
  }
}
