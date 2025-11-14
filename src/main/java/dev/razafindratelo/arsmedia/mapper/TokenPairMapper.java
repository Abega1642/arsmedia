package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RTokenPair;
import dev.razafindratelo.arsmedia.model.token.TokenPair;

public class TokenPairMapper {

  private TokenPairMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static RTokenPair toRest(TokenPair tokenPair) {
    return new RTokenPair(
        TokenMapper.toRest(tokenPair.accessToken()),
        TokenMapper.toRest(tokenPair.refreshToken()),
        tokenPair.requestTime());
  }
}
