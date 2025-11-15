package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.model.ApiClient;
import dev.razafindratelo.arsmedia.repository.model.JApiClient;

public class ApiClientMapper {

  private ApiClientMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static ApiClient toApiClient(JApiClient jApiClient) {
    return new ApiClient(
        jApiClient.getId(),
        jApiClient.getEmail(),
        jApiClient.getPhoneNumber(),
        jApiClient.getClientName());
  }

  public static JApiClient toJApiClient(ApiClient apiClient) {
    return new JApiClient(
        apiClient.id(), apiClient.clientEmail(), apiClient.phoneNumber(), apiClient.clientName());
  }
}
