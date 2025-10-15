package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.mapper.UserMapper;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceIT extends FacadeIT {
  private final String EMAIL = "abega@razafindratelo.dev";
  @Autowired private UserService subject;
  @Autowired private UserRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteByEmail(EMAIL);
  }

  @Test
  void should_throw_exception() {
    assertThrows(IllegalArgumentException.class, () -> subject.findByEmail(null));
    assertThrows(IllegalArgumentException.class, () -> subject.findByEmail(""));
    assertThrows(ConstraintViolationException.class, () -> subject.findByEmail("invalid-email"));
  }

  @Test
  void should_create_an_user_with_success() {
    var expectedUser = createUser();
    var actual = subject.findByEmail(EMAIL);

    assertEquals(expectedUser.getId(), actual.getId());
    assertEquals(expectedUser.getEmail(), actual.getEmail());
    assertEquals(expectedUser.getPassword(), actual.getPassword());
    assertEquals(expectedUser.getPseudo(), actual.getPseudo());
    assertEquals(expectedUser.getPhoneNumber(), actual.getPhoneNumber());
  }

  @Test
  void should_update_pseudo() {
    var user = createUser();
    user.setPseudo("updated-pseudo");

    subject.update(UserMapper.toJUser(user));
    var actual = subject.findByEmail(EMAIL);

    assertEquals(user.getPseudo(), actual.getPseudo());
    assertEquals(user.getEmail(), actual.getEmail());
    assertEquals(user.getId(), actual.getId());
  }

  @Test
  void should_update_activation_status() {
    createUser();

    subject.updateActivationStatusByEmail(EMAIL, true);
    var actual = subject.findByEmail(EMAIL);

    assertTrue(actual.isActivated());
  }

  private User createUser() {
    var user =
        new User(
            UUID.randomUUID().toString(),
            EMAIL,
            "ab3g4",
            "+261 32 92 636 82",
            "https://dummy-bucket-key.com",
            UserRole.ADMIN,
            "dummy-password",
            false,
            now(),
            now());

    return subject.create(UserMapper.toJUser(user));
  }
}
