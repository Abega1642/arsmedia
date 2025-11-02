package dev.razafindratelo.arsmedia.exception;

public class MissingAuthorizationException extends RuntimeException {
  public MissingAuthorizationException(String message) {
    super(message);
  }
}
