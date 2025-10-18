package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.health.model.RUser;
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
  private final String TEST_USER_EMAIL = "user-controller-test@example.com";
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
        new RUser(ADMIN_USER_EMAIL, "admin-user", "+261123456789", UserRole.ADMIN, "password");
    userService.create(adminUser);

    var apiKeyModel = apiKeyService.createAPIKey(ADMIN_USER_EMAIL, Duration.ofDays(10));
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
      userRepository.deleteByEmail(TEST_USER_EMAIL);
    } catch (Exception e) {
      log.error("Cleanup warning: {}", e.getMessage());
    }
  }

  @Test
  void should_get_all_users_with_valid_api_key_and_admin_role() throws Exception {
    var user1 = new RUser("user1@example.com", "user1", "+261111111111", UserRole.USER, "password");
    var user2 = new RUser("user2@example.com", "user2", "+261222222222", UserRole.USER, "password");
    userService.create(user1);
    userService.create(user2);

    mvc.perform(
            get("/users").header("X-API-KEY", adminApiKey).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(3)) // admin + 2 users
        .andExpect(jsonPath("$.content[?(@.email == 'user1@example.com')]").exists())
        .andExpect(jsonPath("$.content[?(@.email == 'user2@example.com')]").exists());
  }

  @Test
  void should_deny_access_to_get_all_users_without_api_key() throws Exception {
    mvc.perform(get("/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("Valid API key required"));
  }

  @Test
  void should_deny_access_to_get_all_users_with_invalid_api_key() throws Exception {
    mvc.perform(get("/users").header("X-API-KEY", "invalid-api-key"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("Valid API key required"));
  }

  @Test
  void should_get_user_by_email_without_authentication() throws Exception {
    var testUser =
        new RUser(TEST_USER_EMAIL, "test-user", "+261333333333", UserRole.USER, "password");
    userService.create(testUser);

    mvc.perform(get("/users/{email}", TEST_USER_EMAIL).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL))
        .andExpect(jsonPath("$.pseudo").value("test-user"))
        .andExpect(jsonPath("$.phoneNumber").value("+261333333333"));
  }

  @Test
  void should_return_not_found_for_non_existent_user_email() throws Exception {
    mvc.perform(get("/users/{email}", "nonexistent@example.com").with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Email not found: nonexistent@example.com"));
  }

  @Test
  void should_create_user_via_sign_up() throws Exception {
    var newUser =
        """
        {
          "email": "newuser@example.com",
          "pseudo": "newuser",
          "phoneNumber": "+261444444444",
          "role": "USER",
          "password": "securepassword123"
        }
        """;

    mvc.perform(
            post("/users/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUser)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.pseudo").value("newuser"))
        .andExpect(jsonPath("$.phoneNumber").value("+261444444444"))
        .andExpect(jsonPath("$.role").value("USER"));

    userRepository.deleteByEmail("newuser@example.com");
  }

  @Test
  void should_return_bad_request_for_invalid_email_on_sign_up() throws Exception {
    var invalidUser =
        """
        {
          "email": "invalid-email",
          "pseudo": "invalid",
          "phoneNumber": "+261555555555",
          "role": "USER",
          "password": "password123"
        }
        """;

    mvc.perform(
            post("/users/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUser)
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_bad_request_for_missing_required_fields_on_sign_up() throws Exception {
    var incompleteUser =
        """
        {
          "email": "incomplete@example.com",
          "role": "USER"
        }
        """;

    mvc.perform(
            post("/users/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteUser)
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_handle_pagination_parameters_correctly() throws Exception {
    for (int i = 0; i < 5; i++) {
      var user =
          new RUser(
              "paguser" + i + "@example.com",
              "paguser" + i,
              "+26160000000" + i,
              UserRole.USER,
              "password");
      userService.create(user);
    }

    mvc.perform(
            get("/users")
                .header("X-API-KEY", adminApiKey)
                .param("page", "0")
                .param("size", "3")
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.pageable.pageNumber").value(0))
        .andExpect(jsonPath("$.pageable.pageSize").value(3));

    for (int i = 0; i < 5; i++) {
      userRepository.deleteByEmail("paguser" + i + "@example.com");
    }
  }
}
