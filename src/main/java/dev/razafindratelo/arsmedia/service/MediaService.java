package dev.razafindratelo.arsmedia.service;

import static java.util.UUID.randomUUID;

import dev.razafindratelo.arsmedia.exception.MediaUploadException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.mapper.VideoMapper;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.Image;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.service.media.VideoMetaDataExtractor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class MediaService {
  private final BucketComponent bucket;
  private final VideoMetaDataExtractor videoExtractor;
  private final VideoRepository videoRepository;
  private final UserService userService;
  private final MultipartFileConverter fileConverter;

  public Video uploadVideo(@NotNull MultipartFile file, @NotNull @Email String userEmail) {
    User owner = validateUserForMediaOperations(userEmail);

    try {
      var videoFile = fileConverter.convert(file);
      var video = videoExtractor.apply(videoFile);
      video.setOwner(owner);

      String bucketKey = "videos/" + randomUUID();

      log.info(
          "Uploading video: id={}, name={}, duration={}, bucketKey={}, user={}",
          video.getId(),
          video.getFileName(),
          video.getDuration(),
          bucketKey,
          userEmail);

      bucket.upload(videoFile, bucketKey);
      video.setFilePath(bucketKey);

      log.info("Video uploaded successfully. Pre-signed URL generated for id={}", video.getId());

      videoRepository.save(VideoMapper.toJVideo(video));

      return video;
    } catch (IOException e) {
      log.error("Failed to upload video for user: {}", userEmail, e);
      throw new MediaUploadException("Failed to upload video : " + e);
    }
  }

  public Audio uploadAudio(@NotNull MultipartFile file, @NotNull @Email String userEmail) {
    validateUserForMediaOperations(userEmail);
    throw new NotImplementedException("Audio upload not yet implemented");
  }

  public Image uploadImage(@NotNull MultipartFile file, @NotNull @Email String userEmail) {
    validateUserForMediaOperations(userEmail);
    throw new NotImplementedException("Image upload not yet implemented");
  }

  private User validateUserForMediaOperations(String userEmail) {
    User user = userService.findByEmail(userEmail);

    log.debug(
        "Validating user for media operations: email={}, isActive={}",
        user.getEmail(),
        user.isActivated());

    if (!user.isActivated()) {
      throw new UserNotActivatedException("User account is not activated: " + userEmail);
    }

    return user;
  }
}
