package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("infra.rabbitmq.exchange", () -> "infra-event-exchange");
    registry.add("infra.rabbitmq.queue", () -> "infra-health-queue");
    registry.add("infra.rabbitmq.routing-key", () -> "infra.event.key");
  }
}
