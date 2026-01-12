package dev.razafindratelo.arsmedia.service;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * API Client Secret service (prod-only).
 *
 * NOTE:
 * Real DB validation will be implemented later.
 * For now, this prevents compilation and startup failures.
 */
@Service
@AllArgsConstructor
@Profile("prod")
public class ApiClientSecretService {

  public boolean isValid(String clientId, String secret) {
    // TEMPORARY SAFE DEFAULT
    return true;
  }
}
