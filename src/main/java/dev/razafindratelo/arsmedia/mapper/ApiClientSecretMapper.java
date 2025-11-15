package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.ApiClientMapper.toApiClient;
import static dev.razafindratelo.arsmedia.mapper.ApiClientMapper.toJApiClient;
import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.model.ApiClientSecret;
import dev.razafindratelo.arsmedia.repository.model.JApiClientSecret;

public class ApiClientSecretMapper {

  private ApiClientSecretMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
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
