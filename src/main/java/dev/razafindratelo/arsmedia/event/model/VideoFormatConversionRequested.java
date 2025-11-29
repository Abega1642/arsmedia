package dev.razafindratelo.arsmedia.event.model;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class VideoFormatConversionRequested extends InfraEvent {
  private final String videoId;
  private final String bucketKey;
  private final String owner;
  private final String jobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(15);
  }
}
