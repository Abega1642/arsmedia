package dev.razafindratelo.arsmedia.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.service.ApiClientSecretService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ApiClientSecretFilterIT extends FacadeIT {
  private static final String USERS_URL = "/users";
  private static final String HOST_HEADER = "Host";
  @Autowired private MockMvc mvc;

  @Autowired private ApiClientSecretService clientService;

  @Test
  void should_allow_public_paths_without_client_secret() throws Exception {
    mvc.perform(get("/ping")).andExpect(status().isOk());
  }

  @Test
  void should_allow_localhost_request_without_client_secret() throws Exception {
    mvc.perform(
            get(USERS_URL)
                .with(
                    req -> {
                      req.setRemoteAddr("127.0.0.1");
                      req.addHeader(HOST_HEADER, "localhost:8080");
                      return req;
                    }))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void should_forbid_external_request_without_client_secret() throws Exception {
    mvc.perform(
            get(USERS_URL)
                .with(
                    r -> {
                      r.setRemoteAddr("8.8.8.8");
                      r.addHeader(HOST_HEADER, "external.com");
                      return r;
                    }))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Invalid or missing client credentials"));
  }
}
