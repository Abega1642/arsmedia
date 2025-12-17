package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.razafindratelo.arsmedia.InfraGenerated;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@InfraGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class InfraEvent implements Serializable {

  private static final int MAX_HANDLER_INIT_DURATION_IN_SECOND = 90;
  private static final SecureRandom sRAND = new SecureRandom();
  @Getter @Setter protected int attemptNb;

  public abstract Duration maxConsumerDuration();

  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(MAX_HANDLER_INIT_DURATION_IN_SECOND);
  }

  public abstract Duration maxConsumerBackoffBetweenRetries();

  public final Duration randomVisibilityTimeout() {
    return eventHandlerInitMaxDuration()
        .plus(maxConsumerDuration())
        .plus(Duration.ofSeconds(sRAND.nextLong(maxConsumerBackoffBetweenRetries().toSeconds())));
  }

  public String getEventSource() {
    return getClass().getSimpleName();
  }
}
