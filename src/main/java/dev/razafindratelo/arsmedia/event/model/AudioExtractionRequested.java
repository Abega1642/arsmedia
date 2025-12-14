package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioExtractionRequested extends InfraEvent {
  private static final int MAX_CONSUMER_DURATION = 5;
  private static final int MAX_CONSUMER_BACKOFF_BETWEEN_RETRIES_DURATION = 15;
  private final String videoId;
  private final String bucketKey;
  private final String owner;
  private final String jobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(MAX_CONSUMER_DURATION);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(MAX_CONSUMER_BACKOFF_BETWEEN_RETRIES_DURATION);
  }
}
