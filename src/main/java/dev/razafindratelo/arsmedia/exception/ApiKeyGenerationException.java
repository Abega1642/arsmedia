package dev.razafindratelo.arsmedia.exception;

public class ApiKeyGenerationException extends RuntimeException {
  public ApiKeyGenerationException(String message) {
    super(message);
  }

  public ApiKeyGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
