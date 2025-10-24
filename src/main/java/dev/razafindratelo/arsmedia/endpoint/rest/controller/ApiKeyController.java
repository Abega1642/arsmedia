package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ApiKeyRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ApiKeyResponse;
import dev.razafindratelo.arsmedia.service.ApiKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/auth/api-keys")
public class ApiKeyController {
  private final ApiKeyService service;

  @PostMapping("/generate")
  public ApiKeyResponse generateApiKey(@NotNull @Valid ApiKeyRequest request) {
    return service.createApiKey(request);
  }
}
