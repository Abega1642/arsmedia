package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeResponse;
import dev.razafindratelo.arsmedia.exception.AuthCodeInvalidException;
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
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@Transactional
@AllArgsConstructor
public class AuthCodeService {
  private AuthCodeRepository repository;
  private UserService userService;
  private Mailer mailer;
  private HtmlTemplateLoader htmlLoader;

  public boolean activateUserProfile(@NotBlank @NotNull String userId, AuthCodeRequest request) {
    var targetUser = userService.findByEmail(request.userEmail());

    if (!userId.equals(targetUser.getId()))
      throw new AuthorizationDeniedException(
          "Can not process user activation due to missing authorization caused by miss matching"
              + " params sent to the system.");

    if (!checkIfAuthCodeIsValid(targetUser.getId(), request.code()))
      throw new AuthCodeInvalidException("The auth code sent is not valid anymore.");

    return userService.updateActivationStatusByEmail(targetUser.getEmail(), true);
  }

  @Transactional
  public AuthCodeResponse sentAuthCodeTo(@NotBlank @NotNull String userId) throws AddressException {
    String sanitizedUserId = userId.replaceAll("[\\r\\n\\t]", "_");
    log.info("Auth code request processing for userId={}", sanitizedUserId);
    var user = userService.findById(sanitizedUserId);

    if (!disableOtherCodeRelatedToUser(user.getId()))
      log.info("Auth code not disabled for user with userId = {}", user.getId());

    var authCode = generate(user.getEmail()).code();

    try {
      var toAddress = new InternetAddress(user.getEmail());
      toAddress.validate();

      var body =
          htmlLoader.apply(
              "auth-code.html", Map.of("CODE", authCode, "USERNAME", user.getPseudo()));

      var subject = "Arsmedia - Authentication Code";
      mailer.accept(
          new dev.razafindratelo.arsmedia.mail.Email(
              toAddress, List.of(), List.of(), subject, body, List.of()));

      log.info("Email sent to userEmail='{}' with subject='{}'", user.getEmail(), subject);

    } catch (AddressException e) {
      throw new AddressException("Invalid email address: {}", user.getEmail());
    }
    return new AuthCodeResponse(user.getEmail(), now());
  }

  public boolean disableOtherCodeRelatedToUser(@NotBlank @NotNull String userId) {
    log.info("Disabling previous auth code related to userId={}", userId);
    return repository.disableAuthCodes(userId) >= 0;
  }

  @Transactional
  public AuthCode generate(@Email @NotBlank @NotNull String email) {
    log.info("Generating auth code for userId={}", email);
    var jUser = userService.findByEmail(email);

    var authCode = AuthCode.generate(jUser);
    var jAuthCode = AuthCodeMapper.toJAuthCode(authCode);

    var saved = repository.save(jAuthCode);
    return AuthCodeMapper.toModel(saved);
  }

  public boolean checkIfAuthCodeIsValid(
      @NotBlank @NotNull String userId, @NotBlank @NotNull String code) {
    log.info("Checking the validity of auth code={} for userId={}", code, userId);
    var authCode = findAuthByUserIdAndCode(userId, code);
    return authCode.deadline().isAfter(now());
  }

  public AuthCode findAuthByUserIdAndCode(
      @NotBlank @NotNull String userId, @NotBlank @NotNull String code) {
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
