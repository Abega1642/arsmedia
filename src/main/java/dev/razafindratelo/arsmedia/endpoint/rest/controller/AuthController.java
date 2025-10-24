package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.LoginRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.LoginResponse;
import dev.razafindratelo.arsmedia.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
  private final AuthService service;

  @PostMapping("login")
  public LoginResponse login(@RequestBody @NotNull @Valid LoginRequest request) {
    return service.logIn(request);
  }
}
