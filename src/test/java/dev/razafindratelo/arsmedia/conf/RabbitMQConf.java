package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@InfraGenerated
@TestConfiguration
public class RabbitMQConf {

  static final RabbitMQContainer rabbit =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

  static {
    rabbit.start();
  }

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("infra.rabbitmq.host", rabbit::getHost);
    registry.add("infra.rabbitmq.port", () -> String.valueOf(rabbit.getAmqpPort()));
    registry.add("infra.rabbitmq.username", rabbit::getAdminUsername);
    registry.add("infra.rabbitmq.password", rabbit::getAdminPassword);
    registry.add("infra.rabbitmq.vhost", () -> "/");
  }
}
