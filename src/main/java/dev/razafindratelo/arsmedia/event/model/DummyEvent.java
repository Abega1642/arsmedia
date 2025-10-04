package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.razafindratelo.arsmedia.InfraGenerated;
import java.time.Duration;
import lombok.Getter;

@InfraGenerated
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyEvent extends InfraEvent {

  private final String uuid;
  private final int waitDurationBeforeConsumingInSeconds;

  @JsonCreator
  public DummyEvent(
      @JsonProperty("uuid") String uuid,
      @JsonProperty("waitDurationBeforeConsumingInSeconds")
          int waitDurationBeforeConsumingInSeconds) {
    this.uuid = uuid;
    this.waitDurationBeforeConsumingInSeconds = waitDurationBeforeConsumingInSeconds;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(waitDurationBeforeConsumingInSeconds);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(5);
  }
}
