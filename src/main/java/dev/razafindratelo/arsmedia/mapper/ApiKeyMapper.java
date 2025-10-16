package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.model.ApiKey;
import dev.razafindratelo.arsmedia.repository.model.JApiKey;

public class ApiKeyMapper {
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
