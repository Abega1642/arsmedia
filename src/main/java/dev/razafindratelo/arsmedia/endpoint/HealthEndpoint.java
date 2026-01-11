package dev.razafindratelo.arsmedia.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthEndpoint {

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
