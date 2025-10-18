package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.transaction.Transactional;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiKeyServiceIT extends FacadeIT {
  private final RUser user =
      new RUser(
          "abega.razafindratelo@gmail.com",
          "+261 32 98 636 82",
          "Abega1642",
          UserRole.ADMIN,
          "random-password");
  @Autowired private ApiKeyService subject;
  @Autowired private UserService userService;

  @BeforeEach
  void setUp() {
    userService.create(user);
  }

  @Test
  @Transactional
  void should_create_an_apiKey_for_the_given_user() {
    var expected = subject.createAPIKey(user.email(), Duration.ofDays(5));
    var actual = subject.findByAPIKeyValue(expected.apiKey());

    assertEquals(expected.id(), actual.id());
    assertEquals(expected.owner().getEmail(), actual.owner().getEmail());
    assertTrue(subject.isApiKeyValid(actual.apiKey()));
  }
}
