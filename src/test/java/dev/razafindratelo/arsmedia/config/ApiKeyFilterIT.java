package dev.razafindratelo.arsmedia.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.ApiKeyRepository;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.service.ApiKeyService;
import dev.razafindratelo.arsmedia.service.UserService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@Slf4j
class ApiKeyFilterIT extends FacadeIT {

  private final String TEST_USER_EMAIL = "apikey-filter-test@example.com";
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private UserService userService;
  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private UserRepository userRepository;
  private String validApiKey;
  private String expiredApiKey;

  @BeforeEach
  void setUp() {
    var adminUser =
        new UserCreationRequest(
            TEST_USER_EMAIL, "test-admin", "+261123456789", UserRole.ADMIN, "password");
    userService.create(adminUser);
    userService.updateActivationStatusByEmail(TEST_USER_EMAIL, true);

    var validApiKeyModel =
        apiKeyService.createApiKeyWithUserEmailAndDuration(TEST_USER_EMAIL, Duration.ofDays(10));
    validApiKey = validApiKeyModel.apiKey();

    var expiredApiKeyModel =
        apiKeyService.createApiKeyWithUserEmailAndDuration(TEST_USER_EMAIL, Duration.ofMillis(1));
    expiredApiKey = expiredApiKeyModel.apiKey();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @AfterEach
  void tearDown() {
    cleanupTestData();
  }

  private void cleanupTestData() {
    try {
      if (validApiKey != null) {
        try {
          var apiKey = apiKeyService.findByAPIKeyValue(validApiKey);
          apiKeyRepository.deleteById(apiKey.id());
        } catch (Exception ignored) {
        }
      }

      if (expiredApiKey != null) {
        try {
          var apiKey = apiKeyService.findByAPIKeyValue(expiredApiKey);
          apiKeyRepository.deleteById(apiKey.id());
        } catch (Exception ignored) {
        }
      }

      userRepository.deleteByEmail(TEST_USER_EMAIL);
    } catch (Exception e) {
      log.error("Cleanup warning: {}", e.getMessage());
    }
  }

  @Test
  void should_allow_access_to_users_endpoint_with_valid_api_key_and_admin_role() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", validApiKey);

    var response =
        restTemplate.exchange("/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void should_deny_access_to_users_endpoint_with_invalid_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", "invalid-api-key-that-does-not-exist");

    var response =
        restTemplate.exchange("/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_deny_access_to_users_endpoint_with_expired_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", expiredApiKey);

    var response =
        restTemplate.exchange("/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_deny_access_to_users_endpoint_without_api_key() {
    var response = restTemplate.getForEntity("/users", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_deny_access_to_users_endpoint_with_valid_api_key_but_non_admin_user() {
    String nonAdminEmail = "non-admin-test2@example.com";

    try {
      var nonAdminUser =
          new UserCreationRequest(
              nonAdminEmail, "non-admin", "+261987654321", UserRole.USER, "password");
      var createdUser = userService.create(nonAdminUser);
      userService.updateActivationStatusByEmail(nonAdminEmail, true);

      var apiKeyModel =
          apiKeyService.createApiKeyWithUserEmailAndDuration(nonAdminEmail, Duration.ofDays(1));
      String nonAdminApiKey = apiKeyModel.apiKey();

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      var headers = new HttpHeaders();
      headers.set("X-API-KEY", nonAdminApiKey);

      var response =
          restTemplate.exchange("/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    } finally {
      try {
        var page = apiKeyService.findAllByUserEmail(nonAdminEmail, 0, 100);
        page.getContent().forEach(apiKey -> apiKeyRepository.deleteById(apiKey.id()));
        userRepository.deleteByEmail(nonAdminEmail);
      } catch (Exception e) {
        log.error("Cleanup warning in non-admin test: {}", e.getMessage());
      }
    }
  }

  @Test
  void should_allow_access_to_public_endpoints_without_api_key() {
    var response = restTemplate.getForEntity("/ping", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
