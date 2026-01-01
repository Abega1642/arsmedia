package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ApiKeyRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ApiKeyResponse;
import dev.razafindratelo.arsmedia.exception.ApiKeyException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.mapper.ApiKeyMapper;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.ApiKey;
import dev.razafindratelo.arsmedia.repository.ApiKeyRepository;
import dev.razafindratelo.arsmedia.repository.model.JApiKey;
import dev.razafindratelo.arsmedia.service.util.ApiKeyGenerator;
import dev.razafindratelo.arsmedia.service.util.Paginator;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class ApiKeyService {
  private final ApiKeyRepository repository;
  private final UserService userService;
  private final ApiKeyGenerator apiKeyGenerator;
  private final Paginator paginator;

  public ApiKeyResponse createApiKey(@NotNull @Valid ApiKeyRequest request) {
    var apiKey = createApiKeyWithUserEmailAndDuration(request.userEmail(), Duration.ofDays(20));
    return new ApiKeyResponse(apiKey.apiKey(), request.reason());
  }

  @Transactional
  public ApiKey createApiKeyWithUserEmailAndDuration(
      @Email @NotNull @NotBlank String userEmail, @NotNull Duration duration) {
    var owner = userService.findByEmail(userEmail);
    log.info("Attempt to generate API key for user {}", userEmail);

    validateUserForApiKeyGeneration(userEmail);
    var creation = now();
    var expiration = creation.plus(duration);
    var apiKeyValue = apiKeyGenerator.apply(owner, creation);

    var apiKey =
        new JApiKey(
            UUID.randomUUID().toString(),
            UserMapper.toJUser(owner),
            apiKeyValue,
            creation,
            expiration);

    log.info("API key generated for user {}", forJava(userEmail));
    return ApiKeyMapper.toModel(repository.save(apiKey));
  }

  private void validateUserForApiKeyGeneration(String userEmail) {
    var user = userService.findByEmail(userEmail);

    log.info(
        "Check user activity for API key generation. User : { email ={}, isActive= {} }",
        user.getEmail(),
        user.isActivated());

    if (!user.isActivated())
      throw new UserNotActivatedException(
          "User account is not activated: %s".formatted(forJava(userEmail)));
  }

  public Page<ApiKey> findAllByUserEmail(
      @Email @NotNull @NotBlank String email, Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    Pageable pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("creation").descending());
    var results = repository.findByOwnerEmail(email, pageable);

    return results.map(ApiKeyMapper::toModel);
  }

  public ApiKey findByAPIKeyValue(@NotBlank @NotNull String apiKey) {
    var jApiKey =
        repository
            .findByApiKey(apiKey)
            .orElseThrow(() -> new ApiKeyException("No API key found with that value"));
    return ApiKeyMapper.toModel(jApiKey);
  }

  public boolean isApiKeyValid(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) return false;

    return repository.findByApiKeyAndNotExpired(apiKey, now()).isPresent();
  }
}
