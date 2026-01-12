package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.repository.ApiClientSecretRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * API Client Secret service.
 *
 * IMPORTANT:
 * This service is ONLY active when the "prod" profile is enabled.
 * This prevents database access during startup on Render / CI.
 */
@Service
@AllArgsConstructor
@Profile("prod")
public class ApiClientSecretService {

  private final ApiClientSecretRepository repository;

  public boolean isValid(
      @NotNull @NotBlank String clientId,
      @NotNull @NotBlank String secret) {

    return repository
        .findByClientId(clientId)
        .map(entity -> entity.getSecret().equals(secret))
        .orElse(false);
  }
}
