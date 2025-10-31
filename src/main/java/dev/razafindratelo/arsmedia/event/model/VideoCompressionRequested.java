package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import java.time.Duration;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoCompressionRequested extends InfraEvent {

  private final String videoId;
  private final String bucketKey;
  private final CompressionOptions compressionOptions;

  @JsonCreator
  public VideoCompressionRequested(
      @JsonProperty("videoId") String videoId,
      @JsonProperty("bucketKey") String bucketKey,
      @JsonProperty("compressionOptions") CompressionOptions compressionOptions) {
    this.videoId = videoId;
    this.bucketKey = bucketKey;
    this.compressionOptions = compressionOptions;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
