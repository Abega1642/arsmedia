package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AuthControllerIT extends FacadeIT {

  private static final String TEST_EMAIL = "a.hello@gmail.com";
  private static final String VALID_PASSWORD = "this_is_the_password";
  private static final MediaType JSON = MediaType.APPLICATION_JSON;

  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;

  private static String login(String password) {
    return """
    {
      "email": "%s",
      "password": "%s"
    }
    """
        .formatted(TEST_EMAIL, password);
  }

  @BeforeEach
  void setUp() {
    var request =
        new UserCreationRequest(
            TEST_EMAIL, "+261 00 0000 000", "abega", UserRole.USER, VALID_PASSWORD);
    userService.create(request);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteByEmail(TEST_EMAIL);
  }

  @Test
  void should_be_a_success_login() throws Exception {
    mvc.perform(post("/auth/login").contentType(JSON).content(login(VALID_PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("SUCCESS"));
  }

  @Test
  void should_denie_authorization() throws Exception {
    mvc.perform(post("/auth/login").contentType(JSON).content(login("fake_password")))
        .andExpect(status().isUnauthorized());
  }
}
