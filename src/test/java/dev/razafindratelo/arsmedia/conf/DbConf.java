package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@InfraGenerated
public class DbConf {
  private final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:13.9"));

  public void start() {
    postgres.start();
  }

  public void stop() {
    postgres.stop();
  }

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }
}
