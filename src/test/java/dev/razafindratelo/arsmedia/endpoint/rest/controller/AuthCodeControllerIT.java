package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthCodeControllerIT extends FacadeIT {
  @Autowired private MockMvc mvc;
  @Autowired private UserService userService;

  @Test
  void should_send_auth_code_email_successfully() throws Exception {
    String mail = "razafindratelo@gmail.com";
    var user =
        new UserCreationRequest(mail, "+261 00 123 44", "abega1642", UserRole.USER, "password");
    var createdUser = userService.create(user);

    mvc.perform(post("/auth/auth-code/{userId}", createdUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sent_to").value(mail));
  }
}
