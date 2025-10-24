package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
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
  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    var user =
        new RUser(
            "a.hello@gmail.com",
            "+261 00 0000 000",
            "abega",
            UserRole.USER,
            "this_is_the_password");
    userService.create(user);
    userService.updateActivationStatusByEmail(user.email(), true);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteByEmail("a.hello@gmail.com");
  }

  @Test
  void should_be_a_success_login() throws Exception {
    var loginRequest =
        """
        {
          "email": "a.hello@gmail.com",
          "password": "this_is_the_password"
        }
        """;

    mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("SUCCESS"));
  }

  @Test
  void should_denie_authorization() throws Exception {
    var loginRequest =
        """
        {
          "email": "a.hello@gmail.com",
          "password": "fake_password"
        }
        """;

    mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginRequest))
        .andExpect(status().isUnauthorized());
  }
}
