package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.Image;
import dev.razafindratelo.arsmedia.model.Video;
import jakarta.transaction.NotSupportedException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class MediaService {
  private final BucketComponent bucket;

  public Video uploadVideo(@NotNull File video) throws NotSupportedException {
    throw new NotSupportedException("NOT IMPLEMENTED");
  }

  public Audio uploadAudio(@NotNull Audio audio) throws NotSupportedException {
    throw new NotSupportedException("NOT IMPLEMENTED");
  }

  public Image uploadImage(@NotNull Image image) throws NotSupportedException {
    throw new NotSupportedException("NOT IMPLEMENTED");
  }
}
