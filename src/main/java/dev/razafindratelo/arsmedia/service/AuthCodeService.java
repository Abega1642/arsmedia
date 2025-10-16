package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.mapper.AuthCodeMapper;
import dev.razafindratelo.arsmedia.model.AuthCode;
import dev.razafindratelo.arsmedia.repository.AuthCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
@AllArgsConstructor
public class AuthCodeService {
  private AuthCodeRepository repository;
  private UserService userService;

  @Transactional
  public AuthCode generate(@Email String email) {
    if (email == null || email.isEmpty())
      throw new IllegalArgumentException("Email is null or empty");

    var jUser = userService.findByEmail(email);

    var authCode = AuthCode.generate(jUser);
    var jAuthCode = AuthCodeMapper.toJAuthCode(authCode);

    var saved = repository.save(jAuthCode);
    return AuthCodeMapper.toModel(saved);
  }

  public boolean checkIfAuthCodeIsValid(String userId, String code) {
    var authCode = findAuthByUserIdAndCode(userId, code);
    return authCode.deadline().isAfter(now());
  }

  public AuthCode findAuthByUserIdAndCode(String userId, String code) {
    if (userId == null || code == null || userId.isEmpty() || code.isEmpty())
      throw new IllegalArgumentException("Email or code can not be empty or null.");

    var jAuthCode =
        repository
            .findByUserIdAndCode(userId, code)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No corresponding authentication code with user id: "
                            + userId
                            + " and code: "
                            + code));

    return AuthCodeMapper.toModel(jAuthCode);
  }
}
