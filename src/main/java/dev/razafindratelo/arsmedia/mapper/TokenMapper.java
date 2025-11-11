package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RToken;
import dev.razafindratelo.arsmedia.model.token.Token;
import dev.razafindratelo.arsmedia.repository.model.token.JToken;

public class TokenMapper {

  private TokenMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Token toModel(JToken jToken) {
    return new Token(
        jToken.getId(),
        UserMapper.toUser(jToken.getUser()),
        jToken.getCreation(),
        jToken.getExpiration(),
        jToken.getType(),
        jToken.isValid(),
        jToken.getValue());
  }

  public static JToken toPersistance(Token token) {
    return new JToken(
        token.id(),
        UserMapper.toJUser(token.user()),
        token.isValid(),
        token.value(),
        token.type(),
        token.creation(),
        token.expiration());
  }

  public static RToken toRest(Token token) {
    return new RToken(
        token.id(),
        token.user().getId(),
        token.creation(),
        token.expiration(),
        token.isValid(),
        token.value());
  }

  public RToken toRest(JToken jToken) {
    return new RToken(
        jToken.getId(),
        jToken.getUser().getId(),
        jToken.getCreation(),
        jToken.getExpiration(),
        jToken.isValid(),
        jToken.getValue());
  }
}
