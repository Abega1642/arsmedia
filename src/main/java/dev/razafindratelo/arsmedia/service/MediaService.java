package dev.razafindratelo.arsmedia.service;

import static java.time.Duration.ofDays;

import dev.razafindratelo.arsmedia.file.BucketComponent;
import dev.razafindratelo.arsmedia.mapper.VideoMapper;
import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.Image;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.service.media.VideoMetaDataExtractor;
import jakarta.transaction.NotSupportedException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@AllArgsConstructor
public class MediaService {
  private final BucketComponent bucket;
  private final String PREFIX_VIDEO_BUCKET_KEY = "videos";
  private final VideoMetaDataExtractor videoExtractor;
  private final VideoRepository videoRepository;

  public Video uploadVideo(@NotNull File video) {
    var uploadedVideo = videoExtractor.apply(video);
    var bucketKey = PREFIX_VIDEO_BUCKET_KEY + "/" + UUID.randomUUID();
    log.info(
        "Uploading video: { id={}, video_name={}, duration={} } of bucket_key={}.",
        uploadedVideo.getId(),
        uploadedVideo.getFileName(),
        uploadedVideo.getDuration(),
        bucketKey);

    bucket.upload(video, bucketKey);
    var url = bucket.presign(bucketKey, ofDays(2));

    log.info("Video uploaded with pre-signed url = {}", url);

    uploadedVideo.setFilePath(url.toString());
    log.info(
        "Saving uploaded video information with pre-signed url = {} and id = {}.",
        url,
        uploadedVideo.getId());
    videoRepository.save(VideoMapper.toJVideo(uploadedVideo));

    return uploadedVideo;
  }

  public Audio uploadAudio(@NotNull Audio audio) throws NotSupportedException {
    throw new NotSupportedException("NOT IMPLEMENTED");
  }

  public Image uploadImage(@NotNull Image image) throws NotSupportedException {
    throw new NotSupportedException("NOT IMPLEMENTED");
  }
}
