package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
public class SwaggerDocIT extends FacadeIT {
  @Autowired private MockMvc mvc;

  @Test
  void should_root_redirects_to_swagger_ui() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/swagger-ui/index.html"));
  }

  @Test
  void should_doc_redirects_to_swagger_ui() throws Exception {
    mvc.perform(get("/doc"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/swagger-ui/index.html"));
  }

  @Test
  void should_swagger_ui_available() throws Exception {
    mvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"));
  }

  @Test
  void should_api_yaml_available() throws Exception {
    mvc.perform(get("/doc/api.yaml"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/yaml"));
  }
}
