package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.exchange", () -> "infra-event-exchange");
    registry.add("spring.rabbitmq.queue", () -> "infra-health-queue");
    registry.add("spring.rabbitmq.routing-key", () -> "spring.event.key");
    registry.add("api.key.signature", () -> "random-signature-for-testing");
    registry.add("app.jwt.secret", () -> "just-test-secret-key");
    registry.add("app.token.access-token-duration", () -> "5");
    registry.add("app.token.max-token-generation-retries", () -> "3");
    registry.add("app.token.access-token-duration", () -> "PT48H");
    registry.add("app.token.refresh-token-duration", () -> "PT168H");
    registry.add("app.token.max-active-tokens-per-user", () -> "5");
    registry.add("app.token.cleanup-cron", () -> "0 0 2 * * ?");
  }
}
