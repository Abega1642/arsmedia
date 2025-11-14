package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import java.time.Duration;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoCompressionRequested extends InfraEvent {

  private final String videoId;
  private final String bucketKey;
  private final String owner;
  private final CompressionOptions compressionOptions;
  private final String jobId;

  @JsonCreator
  public VideoCompressionRequested(
      @JsonProperty("videoId") String videoId,
      @JsonProperty("bucketKey") String bucketKey,
      @JsonProperty("owner") String owner,
      @JsonProperty("compressionOptions") CompressionOptions compressionOptions,
      @JsonProperty("jobId") String jobId) {
    this.videoId = videoId;
    this.bucketKey = bucketKey;
    this.owner = owner;
    this.compressionOptions = compressionOptions;
    this.jobId = jobId;
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
