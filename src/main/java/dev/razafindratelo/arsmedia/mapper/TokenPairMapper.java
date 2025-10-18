package dev.razafindratelo.arsmedia.mapper;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RTokenPair;
import dev.razafindratelo.arsmedia.model.token.TokenPair;

public class TokenPairMapper {
  public static RTokenPair toRest(TokenPair tokenPair) {
    return new RTokenPair(
        TokenMapper.toRest(tokenPair.accessToken()),
        TokenMapper.toRest(tokenPair.refreshToken()),
        tokenPair.requestTime());
  }
}
