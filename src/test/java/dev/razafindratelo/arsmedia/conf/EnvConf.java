package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
public class EnvConf {

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("infra.rabbitmq.exchange", () -> "test-exchange");
    registry.add("infra.rabbitmq.queue", () -> "test-queue");
    registry.add("infra.rabbitmq.routing-key", () -> "test.routing.key");
  }
}
