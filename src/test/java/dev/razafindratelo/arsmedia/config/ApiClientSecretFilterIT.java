package dev.razafindratelo.arsmedia.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.service.ApiClientSecretService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ApiClientSecretFilterIT extends FacadeIT {
  @Autowired private MockMvc mvc;

  @Autowired private ApiClientSecretService clientService;

  @Test
  void should_allow_public_paths_without_client_secret() throws Exception {
    mvc.perform(get("/ping")).andExpect(status().isOk());
  }

  @Test
  void should_allow_localhost_request_without_client_secret() throws Exception {
    mvc.perform(
            get("/users")
                .with(
                    req -> {
                      req.setRemoteAddr("127.0.0.1");
                      req.addHeader("Host", "localhost:8080");
                      return req;
                    }))
        .andExpect(status().isUnauthorized())
        .andDo(result -> assertThat(result.getResponse().getStatus()));
  }

  @Test
  void should_forbid_external_request_without_client_secret() throws Exception {
    mvc.perform(
            get("/users")
                .with(
                    r -> {
                      r.setRemoteAddr("8.8.8.8");
                      r.addHeader("Host", "external.com");
                      return r;
                    }))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Invalid or missing client credentials"));
  }
}
