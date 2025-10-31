package dev.razafindratelo.arsmedia.service;

import static java.lang.String.format;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
@Slf4j
public class VideoService {
  private final VideoRepository repository;
  private final EventProducer<VideoCompressionRequested> eventProducer;

  public void requestCompression(String bucketKey) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    VideoCompressionRequested event =
        new VideoCompressionRequested(videoInst.getId(), bucketKey, CompressionOptions.defaults());

    eventProducer.accept(List.of(event));
    log.info("Video compression event sent for video: {}", videoInst.getId());
  }

  public void requestCompressionWithOptions(String bucketKey, CompressionOptions options) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    VideoCompressionRequested event =
        new VideoCompressionRequested(videoInst.getId(), bucketKey, options);

    eventProducer.accept(List.of(event));
    log.info("Video compression event with custom options sent for video: {}", videoInst.getId());
  }
}
