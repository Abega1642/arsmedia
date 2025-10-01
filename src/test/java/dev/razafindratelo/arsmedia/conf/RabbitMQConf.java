package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@InfraGenerated
public class RabbitMQConf {
  private final RabbitMQContainer rabbit =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

  public void start() {
    rabbit.start();
  }

  public void stop() {
    rabbit.stop();
  }

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", rabbit::getHost);
    registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
  }
}
