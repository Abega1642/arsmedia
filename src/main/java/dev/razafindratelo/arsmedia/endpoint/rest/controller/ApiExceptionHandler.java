package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ErrorResponse;
import dev.razafindratelo.arsmedia.exception.ApiKeyGenerationException;
import dev.razafindratelo.arsmedia.exception.AudioExtractionException;
import dev.razafindratelo.arsmedia.exception.DirectoryUploadException;
import dev.razafindratelo.arsmedia.exception.HmacCalculationException;
import dev.razafindratelo.arsmedia.exception.InvalidAuthorizationFormatException;
import dev.razafindratelo.arsmedia.exception.InvalidFormatConversionException;
import dev.razafindratelo.arsmedia.exception.InvalidTokenException;
import dev.razafindratelo.arsmedia.exception.MissingAuthorizationException;
import dev.razafindratelo.arsmedia.exception.TemplateLoadingException;
import dev.razafindratelo.arsmedia.exception.TokenGenerationException;
import dev.razafindratelo.arsmedia.exception.TokenNotFoundException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.exception.VideoProcessingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, WebRequest request) {

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Required parameter '" + ex.getParameterName() + "' is missing",
            getRequestPath(request),
            "MISSING_REQUIRED_PARAMETER");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
      MissingServletRequestPartException ex, WebRequest request) {

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Required part '" + ex.getRequestPartName() + "' is not present",
            getRequestPath(request),
            "MISSING_REQUIRED_PART");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {

    String message =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST, message, getRequestPath(request), "INVALID_PARAMETER");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request",
            getRequestPath(request),
            "MALFORMED_JSON");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.METHOD_NOT_ALLOWED,
            "HTTP method not supported for this endpoint",
            getRequestPath(request),
            "METHOD_NOT_ALLOWED");

    return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
      AuthorizationDeniedException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.FORBIDDEN, ex.getMessage(), getRequestPath(request), "AUTHORIZATION_DENIED");
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(DirectoryUploadException.class)
  public ResponseEntity<ErrorResponse> handleDirectoryUploadException(
      DirectoryUploadException ex, WebRequest request) {

    log.error("Directory upload failed: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "DIRECTORY_UPLOAD_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(TemplateLoadingException.class)
  public ResponseEntity<ErrorResponse> handleTemplateLoadingException(
      TemplateLoadingException ex, WebRequest request) {

    log.error("Template loading failed: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "TEMPLATE_LOADING_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ApiKeyGenerationException.class)
  public ResponseEntity<ErrorResponse> handleApiKeyGenerationException(
      ApiKeyGenerationException ex, WebRequest request) {

    log.error("API key generation failed: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "API_KEY_GENERATION_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(AudioExtractionException.class)
  public ResponseEntity<ErrorResponse> handleVAudioExtractionException(
      AudioExtractionException ex, WebRequest request) {

    log.error("Audio extraction process failed: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "AUDIO_EXTRACTION_PROCESS_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(VideoProcessingException.class)
  public ResponseEntity<ErrorResponse> handleVideoProcessingException(
      VideoProcessingException ex, WebRequest request) {

    log.error("Video processing failed: {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "VIDEO_PROCESSING_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HmacCalculationException.class)
  public ResponseEntity<ErrorResponse> handleHmacCalculationException(
      HmacCalculationException ex, WebRequest request) {

    log.error("HmacCalculationException : {}", ex.getMessage(), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "HMAC_CALCULATION_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.NOT_FOUND, ex.getMessage(), getRequestPath(request), "ENTITY_NOT_FOUND");
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(TokenGenerationException.class)
  public ResponseEntity<ErrorResponse> handleTokenGenerationException(
      TokenGenerationException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            getRequestPath(request),
            "TOKEN_UNIQUENESS_CONFLICT");
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST, ex.getMessage(), getRequestPath(request), "BAD_FORM");

    return ResponseEntity.badRequest().body(errorResponse);
  }

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

  @ExceptionHandler(InvalidFormatConversionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFormatException(
      InvalidFormatConversionException ex, WebRequest request) {
    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            getRequestPath(request),
            "INVALID_FORMAT_CONVERSION");
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

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, WebRequest request) {

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST, ex.getMessage(), getRequestPath(request), "VALIDATION_ERROR");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  private String getRequestPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return "N/A";
  }
}
