package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeResponse;
import dev.razafindratelo.arsmedia.mail.Mailer;
import dev.razafindratelo.arsmedia.mapper.AuthCodeMapper;
import dev.razafindratelo.arsmedia.model.AuthCode;
import dev.razafindratelo.arsmedia.repository.AuthCodeRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
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
  private Mailer mailer;
  private HtmlTemplateLoader htmlLoader;

  public AuthCodeResponse sentAuthCodeTo(@NotBlank String userId) throws AddressException {
    if (userId == null) throw new IllegalArgumentException("User Id should not be null");

    var user = userService.findById(userId);
    var authCode = generate(user.getEmail()).code();

    try {
      var toAddress = new InternetAddress(user.getEmail());
      toAddress.validate();

      var body =
          htmlLoader.apply(
              "auth-code.html", Map.of("CODE", authCode, "USERNAME", user.getPseudo()));

      mailer.accept(
          new dev.razafindratelo.arsmedia.mail.Email(
              toAddress, List.of(), List.of(), "Arsmedia - Authentication Code", body, List.of()));
    } catch (AddressException e) {
      throw new AddressException("Invalid email address: {}", user.getEmail());
    }
    return new AuthCodeResponse(user.getEmail(), now());
  }

  @Transactional
  public AuthCode generate(@Email @NotBlank String email) {
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
