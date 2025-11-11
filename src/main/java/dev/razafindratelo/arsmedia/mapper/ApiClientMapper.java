package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.model.ApiClient;
import dev.razafindratelo.arsmedia.repository.model.JApiClient;

public class ApiClientMapper {

  private ApiClientMapper() {
    throw new UnsupportedOperationException("Utility class");
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
