package dev.razafindratelo.arsmedia.service;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import dev.razafindratelo.arsmedia.repository.model.JVideo;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CompressionVideoServiceIT {

  public static final String FIRST_VIDEO_ID = randomUUID().toString();
  public static final String SECOND_VIDEO_ID = randomUUID().toString();
  private static final String EXISTING_EMAIL = "user@example.com";
  private static final String NON_EXISTING_EMAIL = "notfound@example.com";
  @Mock private VideoCompressionJobRepository repository;

  @InjectMocks private CompressionVideoService subject;

  private VideoCompressionJob fCmJ;
  private VideoCompressionJob sCmJ;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    fCmJ = createCompressedVideo(randomUUID().toString(), FIRST_VIDEO_ID);
    sCmJ = createCompressedVideo(randomUUID().toString(), SECOND_VIDEO_ID);
  }

  @Test
  void shouldReturnAllCompletedVideosForGivenEmail() {
    when(repository.findAllCompletedByOwnerEmail(EXISTING_EMAIL)).thenReturn(List.of(fCmJ, sCmJ));

    List<Video> result = subject.getAllCompressedVideos(EXISTING_EMAIL);

    assertThat(result)
        .isNotNull()
        .hasSize(2)
        .extracting(Video::getId)
        .containsExactly(FIRST_VIDEO_ID, SECOND_VIDEO_ID);

    verify(repository, times(1)).findAllCompletedByOwnerEmail(EXISTING_EMAIL);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldReturnEmptyListIfNoVideosFound() {
    when(repository.findAllCompletedByOwnerEmail(NON_EXISTING_EMAIL)).thenReturn(List.of());

    List<Video> result = subject.getAllCompressedVideos(NON_EXISTING_EMAIL);

    assertThat(result).isNotNull().isEmpty();

    verify(repository, times(1)).findAllCompletedByOwnerEmail(NON_EXISTING_EMAIL);
    verifyNoMoreInteractions(repository);
  }

  private VideoCompressionJob createCompressedVideo(String compressedId, String videoId) {
    JUser owner = new JUser();
    owner.setId("user-" + CompressionVideoServiceIT.EXISTING_EMAIL.hashCode());
    owner.setEmail(CompressionVideoServiceIT.EXISTING_EMAIL);

    var parent = new JVideo();
    parent.setId(videoId);
    parent.setOwner(owner);

    return new VideoCompressionJob(
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
