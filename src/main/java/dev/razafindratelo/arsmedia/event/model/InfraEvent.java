package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.razafindratelo.arsmedia.InfraGenerated;
import java.io.Serializable;
import java.time.Duration;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;

@InfraGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class InfraEvent implements Serializable {

  private static final Random RANDOM = new Random();
  @Getter @Setter protected int attemptNb;

  public abstract Duration maxConsumerDuration();

  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(90);
  }

  public abstract Duration maxConsumerBackoffBetweenRetries();

  public final Duration randomVisibilityTimeout() {
    long randomSeconds = RANDOM.nextLong(maxConsumerBackoffBetweenRetries().toSeconds() + 1);
    return eventHandlerInitMaxDuration()
        .plus(maxConsumerDuration())
        .plus(Duration.ofSeconds(randomSeconds));
  }

  public String getEventSource() {
    return getClass().getSimpleName();
  }
}
