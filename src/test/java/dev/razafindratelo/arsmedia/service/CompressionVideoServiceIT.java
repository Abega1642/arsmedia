package dev.razafindratelo.arsmedia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.CompressedVideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CompressionVideoServiceTest {

  private static final String EXISTING_EMAIL = "user@example.com";
  private static final String NON_EXISTING_EMAIL = "notfound@example.com";

  @Mock private CompressedVideoRepository repository;

  @InjectMocks private CompressionVideoService service;

  private JCompressedVideo cmp1;
  private JCompressedVideo cmp2;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    cmp1 = createCompressedVideo("cmp1", "video1", EXISTING_EMAIL);
    cmp2 = createCompressedVideo("cmp2", "video2", EXISTING_EMAIL);
  }

  @Test
  void shouldReturnAllCompletedVideosForGivenEmail() {
    when(repository.findAllCompletedByOwnerEmail(EXISTING_EMAIL)).thenReturn(List.of(cmp1, cmp2));

    List<Video> result = service.getAllCompressedVideos(EXISTING_EMAIL);

    assertThat(result)
        .isNotNull()
        .hasSize(2)
        .extracting(Video::getId)
        .containsExactly("video1", "video2");

    verify(repository, times(1)).findAllCompletedByOwnerEmail(EXISTING_EMAIL);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldReturnEmptyListIfNoVideosFound() {
    when(repository.findAllCompletedByOwnerEmail(NON_EXISTING_EMAIL)).thenReturn(List.of());

    List<Video> result = service.getAllCompressedVideos(NON_EXISTING_EMAIL);

    assertThat(result).isNotNull().isEmpty();

    verify(repository, times(1)).findAllCompletedByOwnerEmail(NON_EXISTING_EMAIL);
    verifyNoMoreInteractions(repository);
  }

  private JCompressedVideo createCompressedVideo(
      String compressedId, String videoId, String ownerEmail) {
    JUser owner = new JUser();
    owner.setId("user-" + ownerEmail.hashCode());
    owner.setEmail(ownerEmail);

    var parent = new JVideo();
    parent.setId(videoId);
    parent.setOwner(owner);

    return new JCompressedVideo(
        compressedId,
        parent,
        null,
        LocalDateTime.now(),
        LocalDateTime.now(),
        ProcessStatus.COMPLETED,
        null,
        1);
  }
}
