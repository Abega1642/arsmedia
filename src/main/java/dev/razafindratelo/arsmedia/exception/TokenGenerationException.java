package dev.razafindratelo.arsmedia.exception;

public class TokenGenerationException extends RuntimeException {
  public TokenGenerationException(String message) {
    super(message);
  }

  public TokenGenerationException(String message, Exception e) {
    super(message, e);
  }
}
