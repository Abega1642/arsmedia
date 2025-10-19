package dev.razafindratelo.arsmedia.endpoint.rest.controller.model.token;

import dev.razafindratelo.arsmedia.model.token.TokenType;

public record TokenValidationResult(
    boolean valid, String userEmail, String reason, TokenType actualType) {
  public static TokenValidationResult valid(String userEmail, TokenType actualType) {
    return new TokenValidationResult(true, userEmail, null, actualType);
  }

  public static TokenValidationResult invalid(String reason) {
    return new TokenValidationResult(false, null, reason, null);
  }

  public static TokenValidationResult invalid(String reason, TokenType actualType) {
    return new TokenValidationResult(false, null, reason, actualType);
  }
}
