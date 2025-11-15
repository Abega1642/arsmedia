package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.exception.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class ApiExceptionHandlerIntegrationTest extends FacadeIT {

  private static final String BASE_PATH = "/test";
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private ResultActions expectErrorResponse(
      ResultActions resultActions, HttpStatus status, String errorCode, String path)
      throws Exception {
    return resultActions
        .andExpect(status().is(status.value()))
        .andExpect(jsonPath("$.status").value(status.value()))
        .andExpect(jsonPath("$.error_code").value(errorCode))
        .andExpect(jsonPath("$.path").value(path));
  }

  private ResultActions expectErrorResponseWithMessage(
      ResultActions resultActions, HttpStatus status, String errorCode, String path, String message)
      throws Exception {
    return expectErrorResponse(resultActions, status, errorCode, path)
        .andExpect(jsonPath("$.message").value(message));
  }

  private static class Endpoints {
    static final String REQUIRED_PARAM = BASE_PATH + "/required-param";
    static final String MISSING_PART = BASE_PATH + "/missing-part";
    static final String VALIDATED_PARAM = BASE_PATH + "/validated-param/{email}";
    static final String JSON_BODY = BASE_PATH + "/json-body";
    static final String VALIDATED_BODY = BASE_PATH + "/validated-body";
    static final String GET_ONLY = BASE_PATH + "/get-only";
    static final String MISSING_AUTH = BASE_PATH + "/missing-auth";
    static final String INVALID_AUTH_FORMAT = BASE_PATH + "/invalid-auth-format";
    static final String INVALID_TOKEN = BASE_PATH + "/invalid-token";
    static final String TOKEN_NOT_FOUND = BASE_PATH + "/token-not-found";
    static final String AUTH_FAILED = BASE_PATH + "/auth-failed";
    static final String AUTHORIZATION_DENIED = BASE_PATH + "/authorization-denied";
    static final String USER_NOT_ACTIVATED = BASE_PATH + "/user-not-activated";
    static final String ENTITY_NOT_FOUND = BASE_PATH + "/entity-not-found";
    static final String TOKEN_GENERATION = BASE_PATH + "/token-generation";
    static final String DIRECTORY_UPLOAD = BASE_PATH + "/directory-upload";
    static final String TEMPLATE_LOADING = BASE_PATH + "/template-loading";
    static final String API_KEY_GENERATION = BASE_PATH + "/api-key-generation";
    static final String AUDIO_EXTRACTION = BASE_PATH + "/audio-extraction";
    static final String VIDEO_PROCESSING = BASE_PATH + "/video-processing";
    static final String HMAC_CALCULATION = BASE_PATH + "/hmac-calculation";
    static final String HANDLER_VALIDATION = BASE_PATH + "/handler-validation";
    static final String GENERIC_ERROR = BASE_PATH + "/generic-error";
  }

  private static class ErrorCodes {
    static final String MISSING_REQUIRED_PARAMETER = "MISSING_REQUIRED_PARAMETER";
    static final String MISSING_REQUIRED_PART = "MISSING_REQUIRED_PART";
    static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    static final String MALFORMED_JSON = "MALFORMED_JSON";
    static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    static final String BAD_FORM = "BAD_FORM";
    static final String MISSING_AUTHORIZATION = "MISSING_AUTHORIZATION";
    static final String INVALID_AUTHORIZATION_FORMAT = "INVALID_AUTHORIZATION_FORMAT";
    static final String INVALID_TOKEN = "INVALID_TOKEN";
    static final String TOKEN_NOT_FOUND = "TOKEN_NOT_FOUND";
    static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    static final String AUTHORIZATION_DENIED = "AUTHORIZATION_DENIED";
    static final String USER_DEACTIVATED = "USER_DEACTIVATED";
    static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
    static final String TOKEN_UNIQUENESS_CONFLICT = "TOKEN_UNIQUENESS_CONFLICT";
    static final String DIRECTORY_UPLOAD_FAILED = "DIRECTORY_UPLOAD_FAILED";
    static final String TEMPLATE_LOADING_FAILED = "TEMPLATE_LOADING_FAILED";
    static final String API_KEY_GENERATION_FAILED = "API_KEY_GENERATION_FAILED";
    static final String AUDIO_EXTRACTION_PROCESS_FAILED = "AUDIO_EXTRACTION_PROCESS_FAILED";
    static final String VIDEO_PROCESSING_FAILED = "VIDEO_PROCESSING_FAILED";
    static final String HMAC_CALCULATION_FAILED = "HMAC_CALCULATION_FAILED";
    static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  }

  @RestController
  @RequestMapping(BASE_PATH)
  @Validated
  static class TestController {

    @GetMapping("/required-param")
    public String requiredParam(@RequestParam String requiredParam) {
      return "success";
    }

    @PostMapping("/missing-part")
    public String missingPart(@RequestPart("file") MultipartFile file) {
      return "success";
    }

    @GetMapping("/validated-param/{email}")
    public String validatedParam(@PathVariable @NotBlank String email) {
      return "success";
    }

    @PostMapping("/json-body")
    public String jsonBody(@RequestBody TestRequest request) {
      return "success";
    }

    @PostMapping("/validated-body")
    public String validatedBody(@Valid @RequestBody TestRequest request) {
      return "success";
    }

    @GetMapping("/get-only")
    public String getOnly() {
      return "success";
    }

    @GetMapping("/missing-auth")
    public String missingAuth() {
      throw new MissingAuthorizationException("Authorization header is missing");
    }

    @GetMapping("/invalid-auth-format")
    public String invalidAuthFormat() {
      throw new InvalidAuthorizationFormatException("Invalid authorization format");
    }

    @GetMapping("/invalid-token")
    public String invalidToken() {
      throw new InvalidTokenException("Token is invalid");
    }

    @GetMapping("/token-not-found")
    public String tokenNotFound() {
      throw new TokenNotFoundException("Token not found");
    }

    @GetMapping("/auth-failed")
    public String authFailed() {
      throw new org.springframework.security.core.AuthenticationException(
          "Authentication failed") {};
    }

    @GetMapping("/authorization-denied")
    public String authorizationDenied() {
      AuthorizationDecision decision = new AuthorizationDecision(false);
      throw new AuthorizationDeniedException("Access denied", decision);
    }

    @GetMapping("/user-not-activated")
    public String userNotActivated() {
      throw new UserNotActivatedException("User account is not activated");
    }

    @GetMapping("/entity-not-found")
    public String entityNotFound() {
      throw new EntityNotFoundException("Entity not found");
    }

    @GetMapping("/token-generation")
    public String tokenGeneration() {
      throw new TokenGenerationException("Failed to generate unique token");
    }

    @GetMapping("/directory-upload")
    public String directoryUpload() {
      throw new DirectoryUploadException("Directory upload failed");
    }

    @GetMapping("/template-loading")
    public String templateLoading() {
      throw new TemplateLoadingException("Template loading failed");
    }

    @GetMapping("/api-key-generation")
    public String apiKeyGeneration() {
      throw new ApiKeyGenerationException("API key generation failed");
    }

    @GetMapping("/audio-extraction")
    public String audioExtraction() {
      throw new AudioExtractionException("Audio extraction failed");
    }

    @GetMapping("/video-processing")
    public String videoProcessing() {
      throw new VideoProcessingException("Video processing failed");
    }

    @GetMapping("/hmac-calculation")
    public String hmacCalculation() {
      throw new HmacCalculationException("HMAC calculation failed");
    }

    @GetMapping("/handler-validation")
    public String handlerValidation(@RequestParam @NotBlank String value) {
      return "success";
    }

    @GetMapping("/generic-error")
    public String genericError() {
      throw new RuntimeException("Unexpected error");
    }
  }

  record TestRequest(@NotNull String name, @NotBlank String email) {}

  @Nested
  @DisplayName("Request Parameter Exceptions")
  class RequestParameterExceptionTests {

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException")
    void should_handle_missing_parameter() throws Exception {
      expectErrorResponseWithMessage(
              mockMvc.perform(get(Endpoints.REQUIRED_PARAM)),
              HttpStatus.BAD_REQUEST,
              ErrorCodes.MISSING_REQUIRED_PARAMETER,
              Endpoints.REQUIRED_PARAM,
              "Required parameter 'requiredParam' is missing")
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MissingServletRequestPartException")
    void should_handle_missing_servlet_request_part() throws Exception {
      expectErrorResponseWithMessage(
              mockMvc.perform(multipart(Endpoints.MISSING_PART).param("otherParam", "value")),
              HttpStatus.BAD_REQUEST,
              ErrorCodes.MISSING_REQUIRED_PART,
              Endpoints.MISSING_PART,
              "Required part 'file' is not present")
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException")
    void should_handle_constraint_violation() throws Exception {
      String encodedPath = Endpoints.VALIDATED_PARAM.replace("{email}", "%20");
      expectErrorResponse(
              mockMvc.perform(get(Endpoints.VALIDATED_PARAM.replace("{email}", " "))),
              HttpStatus.BAD_REQUEST,
              ErrorCodes.INVALID_PARAMETER,
              encodedPath)
          .andExpect(jsonPath("$.message", containsString("email")));
    }
  }

  @Nested
  @DisplayName("Request Body Exceptions")
  class RequestBodyExceptionTests {

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException")
    void should_handle_malformed_json() throws Exception {
      var invalidJSON = "not a valid json";
      expectErrorResponseWithMessage(
          mockMvc.perform(
              post(Endpoints.JSON_BODY)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalidJSON)),
          HttpStatus.BAD_REQUEST,
          ErrorCodes.MALFORMED_JSON,
          Endpoints.JSON_BODY,
          "Malformed JSON request");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void should_handle_validation_error() throws Exception {
      TestRequest request = new TestRequest(null, "");

      expectErrorResponse(
          mockMvc.perform(
              post(Endpoints.VALIDATED_BODY)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))),
          HttpStatus.BAD_REQUEST,
          ErrorCodes.BAD_FORM,
          Endpoints.VALIDATED_BODY);
    }
  }

  @Nested
  @DisplayName("HTTP Method Exceptions")
  class HttpMethodExceptionTests {

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException")
    void should_handle_method_not_supported() throws Exception {
      expectErrorResponseWithMessage(
          mockMvc.perform(put(Endpoints.GET_ONLY)),
          HttpStatus.METHOD_NOT_ALLOWED,
          ErrorCodes.METHOD_NOT_ALLOWED,
          Endpoints.GET_ONLY,
          "HTTP method not supported for this endpoint");
    }
  }

  @Nested
  @DisplayName("Authentication and Authorization Exceptions")
  class AuthExceptionTests {

    @Test
    @DisplayName("Should handle MissingAuthorizationException")
    void should_handle_missing_authorization() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.MISSING_AUTH)),
          HttpStatus.UNAUTHORIZED,
          ErrorCodes.MISSING_AUTHORIZATION,
          Endpoints.MISSING_AUTH);
    }

    @Test
    @DisplayName("Should handle InvalidAuthorizationFormatException")
    void should_handle_invalid_auth_format() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.INVALID_AUTH_FORMAT)),
          HttpStatus.BAD_REQUEST,
          ErrorCodes.INVALID_AUTHORIZATION_FORMAT,
          Endpoints.INVALID_AUTH_FORMAT);
    }

    @Test
    @DisplayName("Should handle InvalidTokenException")
    void should_handle_invalid_token() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.INVALID_TOKEN)),
          HttpStatus.UNAUTHORIZED,
          ErrorCodes.INVALID_TOKEN,
          Endpoints.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Should handle TokenNotFoundException")
    void should_handle_token_not_found() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.TOKEN_NOT_FOUND)),
          HttpStatus.UNAUTHORIZED,
          ErrorCodes.TOKEN_NOT_FOUND,
          Endpoints.TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("Should handle AuthenticationException")
    void should_handle_authentication_exception() throws Exception {
      expectErrorResponseWithMessage(
          mockMvc.perform(get(Endpoints.AUTH_FAILED)),
          HttpStatus.UNAUTHORIZED,
          ErrorCodes.AUTHENTICATION_FAILED,
          Endpoints.AUTH_FAILED,
          "Authentication failed");
    }

    @Test
    @DisplayName("Should handle AuthorizationDeniedException")
    void should_handle_authorization_denied() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.AUTHORIZATION_DENIED)),
          HttpStatus.FORBIDDEN,
          ErrorCodes.AUTHORIZATION_DENIED,
          Endpoints.AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("Should handle UserNotActivatedException")
    void should_handle_user_not_activated() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.USER_NOT_ACTIVATED)),
          HttpStatus.FORBIDDEN,
          ErrorCodes.USER_DEACTIVATED,
          Endpoints.USER_NOT_ACTIVATED);
    }
  }

  @Nested
  @DisplayName("Business Logic Exceptions")
  class BusinessLogicExceptionTests {

    @Test
    @DisplayName("Should handle EntityNotFoundException")
    void should_handle_entity_not_found() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.ENTITY_NOT_FOUND)),
          HttpStatus.NOT_FOUND,
          ErrorCodes.ENTITY_NOT_FOUND,
          Endpoints.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("Should handle TokenGenerationException")
    void should_handle_token_generation_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.TOKEN_GENERATION)),
          HttpStatus.CONFLICT,
          ErrorCodes.TOKEN_UNIQUENESS_CONFLICT,
          Endpoints.TOKEN_GENERATION);
    }
  }

  @Nested
  @DisplayName("Processing Exceptions")
  class ProcessingExceptionTests {

    @Test
    @DisplayName("Should handle DirectoryUploadException")
    void should_handle_directory_upload_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.DIRECTORY_UPLOAD)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.DIRECTORY_UPLOAD_FAILED,
          Endpoints.DIRECTORY_UPLOAD);
    }

    @Test
    @DisplayName("Should handle TemplateLoadingException")
    void should_handle_template_loading_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.TEMPLATE_LOADING)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.TEMPLATE_LOADING_FAILED,
          Endpoints.TEMPLATE_LOADING);
    }

    @Test
    @DisplayName("Should handle ApiKeyGenerationException")
    void should_handle_api_key_generation_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.API_KEY_GENERATION)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.API_KEY_GENERATION_FAILED,
          Endpoints.API_KEY_GENERATION);
    }

    @Test
    @DisplayName("Should handle AudioExtractionException")
    void should_handle_audio_extraction_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.AUDIO_EXTRACTION)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.AUDIO_EXTRACTION_PROCESS_FAILED,
          Endpoints.AUDIO_EXTRACTION);
    }

    @Test
    @DisplayName("Should handle VideoProcessingException")
    void should_handle_video_processing_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.VIDEO_PROCESSING)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.VIDEO_PROCESSING_FAILED,
          Endpoints.VIDEO_PROCESSING);
    }

    @Test
    @DisplayName("Should handle HmacCalculationException")
    void should_handle_hmac_calculation_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.HMAC_CALCULATION)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.HMAC_CALCULATION_FAILED,
          Endpoints.HMAC_CALCULATION);
    }
  }

  @Nested
  @DisplayName("Validation Exceptions")
  class ValidationExceptionTests {

    @Test
    @DisplayName("Should handle HandlerMethodValidationException")
    void should_handle_handler_method_validation_exception() throws Exception {
      expectErrorResponse(
          mockMvc.perform(get(Endpoints.HANDLER_VALIDATION).param("value", "")),
          HttpStatus.BAD_REQUEST,
          ErrorCodes.INVALID_PARAMETER,
          Endpoints.HANDLER_VALIDATION);
    }
  }

  @Nested
  @DisplayName("Generic Exception")
  class GenericExceptionTests {

    @Test
    @DisplayName("Should handle generic Exception")
    void should_handle_generic_exception() throws Exception {
      expectErrorResponseWithMessage(
          mockMvc.perform(get(Endpoints.GENERIC_ERROR)),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCodes.INTERNAL_SERVER_ERROR,
          Endpoints.GENERIC_ERROR,
          "An internal server error occurred");
    }
  }
}
