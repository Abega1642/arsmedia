package dev.razafindratelo.arsmedia.service.media;

import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VideoMetaDataExtractorTest {
  private static final String PREFIX = "/videos/";
  private static final String VID_MP4 = "test-video-one.mp4";
  private VideoMetaDataExtractor subject;

  @BeforeEach
  void setUp() throws IOException {
    try {
      subject = new VideoMetaDataExtractor();
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  @Test
  void should_be_able_to_detect_type_webm_from_a_video() throws URISyntaxException {
    var resource = getClass().getResource(PREFIX + "test-video-one.webm");
    assertNotNull(resource);

    File video = new File(resource.toURI());
    var actual = subject.apply(video);

    assertNotNull(actual);
    assertTrue(
        actual.getContainerFormat() == ContainerFormat.WEBM
            || actual.getContainerFormat() == ContainerFormat.MKV);
  }

  @Test
  void should_extract_basic_video_metadata() throws URISyntaxException {
    var resource = getClass().getResource(PREFIX + VID_MP4);
    assertNotNull(resource);

    File video = new File(resource.toURI());
    var actual = subject.apply(video);

    assertNotNull(actual);
    assertEquals(FileType.VIDEO, actual.getFileType());
    assertNotNull(actual.getFileName());
    assertTrue(actual.getSize() > 0);
    assertTrue(actual.getDuration() > 0);
    assertTrue(actual.getWidth() > 0);
    assertTrue(actual.getHeight() > 0);
  }

  @Test
  void should_handle_video_without_audio_stream() throws URISyntaxException {
    var resource = getClass().getResource(PREFIX + VID_MP4);
    assertNotNull(resource);

    File video = new File(resource.toURI());
    var actual = subject.apply(video);

    assertNotNull(actual);
    assertNotNull(actual.getCodec());
    assertNull(actual.getAudioCodec());
    assertEquals(0, actual.getAudioChannels());
  }

  @Test
  void should_throw_when_file_not_exists() {
    File missingFile = new File("/non/existent/video.mp4");
    assertThrows(UncheckedIOException.class, () -> subject.apply(missingFile));
  }
}
