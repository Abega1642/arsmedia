package dev.razafindratelo.arsmedia.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomHealthController {

    @GetMapping("/custom-health")
    public String health() {
        return "OK";
    }
}
