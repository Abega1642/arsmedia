package dev.razafindratelo.arsmedia.event.model;

import static java.lang.Math.random;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.razafindratelo.arsmedia.InfraGenerated;
import java.io.Serializable;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@InfraGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class InfraEvent implements Serializable {

  @Getter @Setter protected int attemptNb;

  public abstract Duration maxConsumerDuration();

  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(90);
  }

  public abstract Duration maxConsumerBackoffBetweenRetries();

  public final Duration randomVisibilityTimeout() {
    return eventHandlerInitMaxDuration()
        .plus(maxConsumerDuration())
        .plus(
            Duration.ofSeconds((long) (random() * maxConsumerBackoffBetweenRetries().toSeconds())));
  }

  public String getEventSource() {
    return getClass().getSimpleName();
  }
}
