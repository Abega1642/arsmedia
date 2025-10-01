package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@InfraGenerated
@SuppressWarnings("all")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Slf4j
public class FacadeIT {

  private static final DbConf DB_CONF = new DbConf();
  private static final RabbitMQConf RABBIT_CONF = new RabbitMQConf();

  @BeforeAll
  static void beforeAll() {
    DB_CONF.start();
    RABBIT_CONF.start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  DB_CONF.stop();
                  RABBIT_CONF.stop();
                }));
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    DB_CONF.configureProperties(registry);
    RABBIT_CONF.configureProperties(registry);

    try {
      var envConfClazz = Class.forName("dev.razafindratelo.arsmedia.conf.EnvConf");
      var configureMethod =
          envConfClazz.getDeclaredMethod("configureProperties", DynamicPropertyRegistry.class);
      var envConfInstance = envConfClazz.getConstructor().newInstance();
      configureMethod.invoke(envConfInstance, registry);
    } catch (ClassNotFoundException e) {
      log.warn("EnvConf missing: no project-specific test env vars will be set");
    } catch (Exception e) {
      log.error("Failed to apply EnvConf", e);
    }
  }
}
