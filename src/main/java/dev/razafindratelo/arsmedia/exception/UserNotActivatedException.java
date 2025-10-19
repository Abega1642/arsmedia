package dev.razafindratelo.arsmedia.exception;

public class UserNotActivatedException extends RuntimeException {
  public UserNotActivatedException(String message) {
    super(message);
  }
}
