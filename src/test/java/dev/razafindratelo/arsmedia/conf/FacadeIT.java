package dev.razafindratelo.arsmedia.conf;

import dev.razafindratelo.arsmedia.InfraGenerated;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@InfraGenerated
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import({PostgresqlConf.class, RabbitMQConf.class, EnvConf.class})
public class FacadeIT {

  @Test
  void contextLoads() {
    log.info("Integration Test context loaded with DB + RabbitMQ + EnvConf");
  }
}
