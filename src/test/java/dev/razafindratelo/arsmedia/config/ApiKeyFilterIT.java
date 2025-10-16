package dev.razafindratelo.arsmedia.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.conf.TestSecurityConf;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.TestApiKeyController;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.health.model.RUser;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.ApiKeyRepository;
import dev.razafindratelo.arsmedia.repository.model.JApiKey;
import dev.razafindratelo.arsmedia.service.ApiKeyService;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;

@Import({TestApiKeyController.class, TestSecurityConf.class})
class ApiKeyFilterIT extends FacadeIT {
  private static final String TEST_USER_EMAIL = "apikey-filter-test@example.com";
  private static final int BATCH_SIZE = 100;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private UserService userService;
  private String validApiKey;
  private String expiredApiKey;

  @BeforeEach
  void setUp() {
    deleteApiKeysByOwnerEmail(TEST_USER_EMAIL);

    try {
      userService.findByEmail(TEST_USER_EMAIL);
    } catch (EntityNotFoundException e) {
      var testUser = new RUser(TEST_USER_EMAIL, "--", "-----", UserRole.USER, "random-password");
      userService.create(testUser);
    }

    var validApiKeyModel = apiKeyService.createAPIKey(TEST_USER_EMAIL, Duration.ofDays(10));
    validApiKey = validApiKeyModel.apiKey();

    var expiredApiKeyModel = apiKeyService.createAPIKey(TEST_USER_EMAIL, Duration.ofMillis(1));
    expiredApiKey = expiredApiKeyModel.apiKey();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @AfterEach
  void tearDown() {
    deleteApiKeysByOwnerEmail(TEST_USER_EMAIL);
  }

  private void deleteApiKeysByOwnerEmail(String email) {
    int pageNumber = 0;
    Page<JApiKey> page;

    do {
      Pageable pageable = PageRequest.of(pageNumber++, BATCH_SIZE);
      page = apiKeyRepository.findByOwnerEmail(email, pageable);
      apiKeyRepository.deleteAll(page.getContent());
    } while (page.hasNext());
  }

  @Test
  void should_allow_access_to_secure_endpoint_with_valid_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", validApiKey);

    var response =
        restTemplate.exchange(
            "/test/api-key/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("secure-data");
  }

  @Test
  void should_deny_access_to_secure_endpoint_with_invalid_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", "invalid-api-key-that-definitely-does-not-exist");

    var response =
        restTemplate.exchange(
            "/test/api-key/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_deny_access_to_secure_endpoint_with_expired_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", expiredApiKey);

    var response =
        restTemplate.exchange(
            "/test/api-key/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_deny_access_to_secure_endpoint_without_api_key() {
    var response = restTemplate.getForEntity("/test/api-key/secure", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void should_allow_access_to_public_endpoint_without_api_key() {
    var response = restTemplate.getForEntity("/test/api-key/public", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("public-data");
  }

  @Test
  void should_allow_access_to_public_endpoint_with_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", validApiKey);

    var response =
        restTemplate.exchange(
            "/test/api-key/public", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("public-data");
  }

  @Test
  void should_deny_access_to_admin_endpoint_without_admin_role() {
    var headers = new HttpHeaders();
    headers.set("X-API-KEY", validApiKey);

    var response =
        restTemplate.exchange(
            "/test/api-key/admin-secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
