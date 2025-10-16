package dev.razafindratelo.arsmedia.service;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.exception.APIKeyException;
import dev.razafindratelo.arsmedia.mapper.ApiKeyMapper;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.ApiKey;
import dev.razafindratelo.arsmedia.repository.ApiKeyRepository;
import dev.razafindratelo.arsmedia.repository.model.JApiKey;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class ApiKeyService {
  private final ApiKeyRepository repository;
  private final UserService userService;
  private final ApiKeyGenerator apiKeyGenerator;
  private final Pagination paginator;

  @Transactional
  public ApiKey createAPIKey(String userEmail, Duration duration) {
    var owner = userService.findByEmail(userEmail);
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
    return ApiKeyMapper.toModel(repository.save(apiKey));
  }

  public Page<ApiKey> findAllByUserEmail(@Email String email, Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    Pageable pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());
    var results = repository.findByOwnerEmail(email, pageable);

    return results.map(ApiKeyMapper::toModel);
  }

  public ApiKey findByAPIKeyValue(String apiKey) {
    if (apiKey == null || apiKey.isEmpty())
      throw new APIKeyException("API key value should not be null or empty");

    var jApiKey =
        repository
            .findByApiKey(apiKey)
            .orElseThrow(() -> new APIKeyException("No API key found with that value"));
    return ApiKeyMapper.toModel(jApiKey);
  }

  public boolean isApiKeyValid(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) return false;

    return repository.findByApiKeyAndNotExpired(apiKey, now()).isPresent();
  }
}
