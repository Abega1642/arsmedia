package dev.razafindratelo.arsmedia.conf;

import static java.lang.Runtime.getRuntime;

import dev.razafindratelo.arsmedia.InfraGenerated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@InfraGenerated
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
public abstract class FacadeIT {

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();
  private static final RabbitMQConf RABBITMQ_CONF = new RabbitMQConf();
  private static final BucketConf BUCKET_CONF = new BucketConf();

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    RABBITMQ_CONF.start();
    BUCKET_CONF.start();

    getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  POSTGRES_CONF.stop();
                  RABBITMQ_CONF.stop();
                  BUCKET_CONF.stop();
                }));
  }

  @SneakyThrows
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
    RABBITMQ_CONF.configureProperties(registry);
    BUCKET_CONF.configureProperties(registry);
    new EnvConf().configureProperties(registry);

    try {
      var envConfClazz = Class.forName("dev.razafindratelo.arsmedia.conf.EnvConf");
      var envConfConfigureProperties =
          envConfClazz.getDeclaredMethod("configureProperties", DynamicPropertyRegistry.class);
      var envConf = envConfClazz.getConstructor().newInstance();
      envConfConfigureProperties.invoke(envConf, registry);
    } catch (ClassNotFoundException e) {
      log.warn("EnvConf missing: no project-specific test env vars will be set");
    }
  }
}
