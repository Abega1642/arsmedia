package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ApiKeyServiceIT extends FacadeIT {
  private final UserCreationRequest user =
      new UserCreationRequest(
          "api.key@gmail.com", "+261 32 98 636 82", "Abega1642", UserRole.ADMIN, "random-password");
  @Autowired private ApiKeyService subject;
  @Autowired private UserRepository userRepository;
  @Autowired private UserService userService;
  @Autowired private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    userService.create(user);
  }

  @Test
  @Transactional
  void should_create_an_apiKey_for_the_given_user() {
    userService.updateActivationStatusByEmail(user.email(), true);
    entityManager.flush();
    entityManager.clear();

    var expected = subject.createApiKeyWithUserEmailAndDuration(user.email(), Duration.ofDays(5));
    var actual = subject.findByAPIKeyValue(expected.apiKey());

    assertEquals(expected.id(), actual.id());
    assertEquals(expected.owner().getEmail(), actual.owner().getEmail());
    assertTrue(subject.isApiKeyValid(actual.apiKey()));
  }

  @Test
  @Transactional
  void should_not_allow_apiKey_generation_for_not_activated_account() {
    assertThrows(
        UserNotActivatedException.class,
        () -> subject.createApiKeyWithUserEmailAndDuration(user.email(), Duration.ofDays(5)));
    userRepository.deleteByEmail(user.email());
  }
}
