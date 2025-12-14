package dev.razafindratelo.arsmedia.event.model;

import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class VideoFormatConversionRequested extends InfraEvent {
  private static final int MAX_CONSUMER_DURATION = 5;
  private static final int MAX_CONSUMER_BACKOFF_BETWEEN_RETRIES_DURATION = 15;

  private final String videoId;
  private final String bucketKey;
  private final String owner;
  private final ContainerFormat targetFormat;
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
