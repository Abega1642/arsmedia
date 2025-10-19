package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ErrorResponse;
import dev.razafindratelo.arsmedia.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class ApiException {
  @ExceptionHandler(MissingAuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleMissingAuthorization(
      MissingAuthorizationException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            getRequestPath(request),
            "MISSING_AUTHORIZATION");
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(InvalidAuthorizationFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidAuthorizationFormat(
      InvalidAuthorizationFormatException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            getRequestPath(request),
            "INVALID_AUTHORIZATION_FORMAT");
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidToken(
      InvalidTokenException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED, ex.getMessage(), getRequestPath(request), "INVALID_TOKEN");
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(TokenNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTokenNotFound(
      TokenNotFoundException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED, ex.getMessage(), getRequestPath(request), "TOKEN_NOT_FOUND");
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(UserNotActivatedException.class)
  public ResponseEntity<ErrorResponse> handleUserNotActivated(
      UserNotActivatedException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.FORBIDDEN, ex.getMessage(), getRequestPath(request), "USER_DEACTIVATED");
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED,
            "Authentication failed",
            getRequestPath(request),
            "AUTHENTICATION_FAILED");
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An internal server error occurred",
            getRequestPath(request),
            "INTERNAL_SERVER_ERROR");
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private String getRequestPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getServletPath();
    }
    return "N/A";
  }
}
