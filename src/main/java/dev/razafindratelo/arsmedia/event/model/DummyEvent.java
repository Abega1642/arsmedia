package dev.razafindratelo.arsmedia.event.model;

import dev.razafindratelo.arsmedia.InfraGenerated;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@InfraGenerated
@AllArgsConstructor
@Getter
public class DummyEvent extends InfraEvent {
  private final String uuid;
  private final int waitDurationBeforeConsumingInSeconds;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(waitDurationBeforeConsumingInSeconds);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(5);
  }
}
