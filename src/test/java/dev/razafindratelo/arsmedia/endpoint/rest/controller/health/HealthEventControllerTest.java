package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
class HealthEventControllerTest extends FacadeIT {

  @Autowired private MockMvc mvc;

  @Test
  void should_trigger_dummy_events_and_return_uuids() throws Exception {
    int nbEvent = 3;
    int waitInSeconds = 1;

    mvc.perform(
            get("/health/message")
                .param("nbEvent", String.valueOf(nbEvent))
                .param("waitInSeconds", String.valueOf(waitInSeconds)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(nbEvent))
        .andExpect(jsonPath("$[0]").exists())
        .andExpect(jsonPath("$[1]").exists())
        .andExpect(jsonPath("$[2]").exists());
  }
}
