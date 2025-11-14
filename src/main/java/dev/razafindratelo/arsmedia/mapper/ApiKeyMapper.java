package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.model.ApiKey;
import dev.razafindratelo.arsmedia.repository.model.JApiKey;

public class ApiKeyMapper {

  private ApiKeyMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static ApiKey toModel(JApiKey jApiKey) {
    return new ApiKey(
        jApiKey.getId(),
        UserMapper.toUser(jApiKey.getOwner()),
        jApiKey.getApiKey(),
        jApiKey.getCreation(),
        jApiKey.getExpiration());
  }

  public static JApiKey toJApiKey(ApiKey apiKey) {
    return new JApiKey(
        apiKey.id(),
        UserMapper.toJUser(apiKey.owner()),
        apiKey.apiKey(),
        apiKey.creation(),
        apiKey.expiration());
  }
}
