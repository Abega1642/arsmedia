package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
class UserControllerIT extends FacadeIT {
  private static final String USERS_URL = "/users";
  private static final String X_API_KEY_HEADER = "X-API-KEY";
  private static final String SIGN_UP_URL = "/sign-up";
  private static final String PAGE_PARAM = "page";
  private static final String SIZE_PARAM = "size";
  private static final String zero = "0";
  private final String ADMIN_USER_EMAIL = "admin-controller-test@example.com";
  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private UserRepository userRepository;
  @Autowired private ApiKeyRepository apiKeyRepository;
  private String adminApiKey;

  @BeforeEach
  void setUp() {
    var adminUser =
        new UserCreationRequest(
            ADMIN_USER_EMAIL,
            "admin-user",
            "+261123456789",
            UserRole.ADMIN,
            randomUUID().toString());
    userService.create(adminUser);
    userService.updateActivationStatusByEmail(ADMIN_USER_EMAIL, true);

    var apiKeyModel =
        apiKeyService.createApiKeyWithUserEmailAndDuration(ADMIN_USER_EMAIL, Duration.ofDays(10));
    adminApiKey = apiKeyModel.apiKey();
  }

  @AfterEach
  void tearDown() {
    cleanupTestData();
  }

  private void cleanupTestData() {
    try {
      if (adminApiKey != null) {
        apiKeyRepository
            .findByApiKey(adminApiKey)
            .ifPresent(apiKey -> apiKeyRepository.deleteById(apiKey.getId()));
      }
      userRepository.deleteByEmail(ADMIN_USER_EMAIL);
      String testUserEmail = "user-controller-test@example.com";
      userRepository.deleteByEmail(testUserEmail);
    } catch (Exception e) {
      log.error("Cleanup warning: {}", e.getMessage());
    }
  }

  @Test
  void should_deny_access_to_get_all_users_without_api_key() throws Exception {
    mvc.perform(get(USERS_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_deny_access_to_get_all_users_with_invalid_api_key() throws Exception {
    mvc.perform(get(USERS_URL).header(X_API_KEY_HEADER, "invalid-api-key"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_create_user_via_sign_up() throws Exception {
    var newUser =
        """
        {
          "email": "newuser@example.com",
          "pseudo": "newuser",
          "phone_number": "+261444444444",
          "role": "USER",
          "password": "securepassword123"
        }
        """;

    var email = "newuser@example.com";
    mvc.perform(
            post(SIGN_UP_URL).contentType(MediaType.APPLICATION_JSON).content(newUser).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.pseudo").value("newuser"))
        .andExpect(jsonPath("$.phone_number").value("+261444444444"))
        .andExpect(jsonPath("$.role").value("USER"));

    userRepository.deleteByEmail(email);
  }

  @Test
  void should_return_bad_request_for_invalid_email_on_sign_up() throws Exception {
    var invalidUser =
        """
        {
          "email": "invalid-email",
          "pseudo": "invalid",
          "phone_number": "+261555555555",
          "role": "USER",
          "password": "password123"
        }
        """;

    mvc.perform(
            post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUser)
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
