package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.repository.ApiClientSecretRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApiClientSecretService {
  private final ApiClientSecretRepository repository;

  public boolean isValid(@NotNull @NotBlank String clientId, @NotNull @NotBlank String secret) {
    return repository.findByApiClientIdAndSecret(clientId, secret).isPresent();
  }
}
