package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.ApiClientMapper.toApiClient;
import static dev.razafindratelo.arsmedia.mapper.ApiClientMapper.toJApiClient;

import dev.razafindratelo.arsmedia.model.ApiClientSecret;
import dev.razafindratelo.arsmedia.repository.model.JApiClientSecret;

public class ApiClientSecretMapper {

  private ApiClientSecretMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static ApiClientSecret toApiClientSecret(JApiClientSecret jApiClientSecret) {
    return new ApiClientSecret(
        jApiClientSecret.getId(),
        toApiClient(jApiClientSecret.getApiClient()),
        jApiClientSecret.getSecret());
  }

  public static JApiClientSecret toJApiClientSecret(ApiClientSecret apiClientSecret) {
    return new JApiClientSecret(
        apiClientSecret.id(), toJApiClient(apiClientSecret.apiClient()), apiClientSecret.secret());
  }
}
