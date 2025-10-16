package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.config.RequiresApiKey;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestApiKeyController {

  @GetMapping("/test/api-key/secure")
  @RequiresApiKey
  public String secureEndpoint() {
    return "secure-data";
  }

  @GetMapping("/test/api-key/public")
  public String publicEndpoint() {
    return "public-data";
  }

  @GetMapping("/test/api-key/admin-secure")
  @PreAuthorize("hasRole('ADMIN')")
  @RequiresApiKey
  public String adminSecureEndpoint() {
    return "admin-secure-data";
  }
}
