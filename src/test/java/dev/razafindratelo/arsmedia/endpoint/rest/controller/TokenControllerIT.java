package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class TokenControllerIT extends FacadeIT {
  private static final String AUTH_TOKEN_TOKEN_URL = "/auth/token/token-pairs";
  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;

  @Test
  void should_not_generate_token_pairs_for_ukown_user_id() throws Exception {
    var tokenRequest =
        """
        {
        "user_id": "fakeId",
        "user_email": "fake@email.com"
        }
        """;

    mvc.perform(
            post(AUTH_TOKEN_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenRequest)
                .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_not_generate_token_pairs_even_for_an_existing_user_but_not_activated()
      throws Exception {
    String userEmail = "dev.razafindratelo@gmail.com";
    var userId = createTestUser(userEmail);

    var tokenRequest = getJsonRequest(userId, userEmail);

    mvc.perform(
            post(AUTH_TOKEN_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenRequest)
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_not_generate_token_pairs_with_success() throws Exception {
    var userEmail = "razafindratelo.abega@gmail.com";
    var userId = createTestUser(userEmail);
    userService.updateActivationStatusByEmail(userEmail, true);

    var tokenRequest = getJsonRequest(userId, userEmail);

    mvc.perform(
            post(AUTH_TOKEN_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenRequest)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.refresh_token").exists())
        .andExpect(jsonPath("$.request_time").exists());
  }

  @Test
  void should_generate_token_pairs_for_refresh_token() throws Exception {
    var userEmail = "test@dev.com";

    var userId = createTestUser(userEmail);
    userService.updateActivationStatusByEmail(userEmail, true);

    var tokenRequest = getJsonRequest(userId, userEmail);

    MvcResult result =
        mvc.perform(
                post(AUTH_TOKEN_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(tokenRequest)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    String refreshToken = read(jsonResponse, "$.refresh_token.value");

    mvc.perform(
            post("/auth/token/refresh-token-pairs")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("refresh_token", refreshToken)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.refresh_token").exists())
        .andExpect(jsonPath("$.request_time").exists());
  }

  private String createTestUser(String userEmail) {
    var testUser =
        new UserCreationRequest(
            userEmail, "+261 00 00 000 00", "abega1642", UserRole.USER, "password");
    return userService.create(testUser).getId();
  }

  private String getJsonRequest(String userId, String userEmail) {
    return " {\n\t\"user_id\": \"" + userId + "\",\n\t\"user_email\": \"" + userEmail + "\"\n}";
  }
}
