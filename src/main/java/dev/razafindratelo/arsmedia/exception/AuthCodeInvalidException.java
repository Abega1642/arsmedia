package dev.razafindratelo.arsmedia.exception;

public class AuthCodeInvalidException extends RuntimeException {
  public AuthCodeInvalidException(String message) {
    super(message);
  }
}
