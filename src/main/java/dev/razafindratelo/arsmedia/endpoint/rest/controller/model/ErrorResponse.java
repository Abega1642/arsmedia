package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import static java.time.LocalDateTime.now;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    String errorCode) {
  public static ErrorResponse of(HttpStatus status, String message, String path) {
    return new ErrorResponse(now(), status.value(), status.getReasonPhrase(), message, path, null);
  }

  public static ErrorResponse of(HttpStatus status, String message, String path, String errorCode) {
    return new ErrorResponse(
        now(), status.value(), status.getReasonPhrase(), message, path, errorCode);
  }
}
