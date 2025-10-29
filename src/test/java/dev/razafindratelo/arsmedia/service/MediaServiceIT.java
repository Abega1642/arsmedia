package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import java.io.File;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class MediaServiceIT extends FacadeIT {
  @Autowired private MediaService subject;

  @Test
  void should_upload_video_successfully() throws URISyntaxException {
    var resource = getClass().getResource("/videos/test-video-one.webm");
    assertNotNull(resource);

    var subjectVideo = new File(resource.toURI());
    var actual = subject.uploadVideo(subjectVideo);

    log.info(actual.toString());

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getFilePath());
  }
}
