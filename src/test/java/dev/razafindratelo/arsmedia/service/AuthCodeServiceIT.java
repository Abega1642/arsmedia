package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.AuthCodeRepository;
import dev.razafindratelo.arsmedia.repository.mapper.AuthCodeMapper;
import dev.razafindratelo.arsmedia.repository.mapper.UserMapper;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthCodeServiceIT extends FacadeIT {
  @Autowired private AuthCodeService subject;
  @Autowired private UserService userService;
  @Autowired private AuthCodeRepository repository;

  @Test
  @Transactional
  void should_generate_auth_code() {
    var email = "a.razafindratelo@gmail.com";
    var user =
        new User(
            UUID.randomUUID().toString(),
            email,
            "ab3g4",
            "+261 32 92 636 82",
            "https://dummy-bucket-key.com",
            UserRole.ADMIN,
            "dummy-password",
            false,
            now(),
            now());

    var savedUser = userService.create(UserMapper.toJUser(user));

    var expected = subject.generate(email);

    var actualAuthCode =
        repository
            .findByUserIdAndCode(savedUser.getId(), expected.code())
            .orElseThrow(() -> new AssertionError("Auth code not found"));

    var actual = AuthCodeMapper.toModel(actualAuthCode);

    assertEquals(expected, actual);
    assertEquals(expected.owner(), actual.owner());
    assertTrue(subject.checkIfAuthCodeIsValid(user.getId(), actual.code()));
  }
}
