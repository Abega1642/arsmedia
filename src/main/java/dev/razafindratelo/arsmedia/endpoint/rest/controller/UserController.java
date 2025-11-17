package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.config.RequiresApiKey;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Validated
public class UserController {
  private final UserService service;

  @PreAuthorize("hasRole('ADMIN')")
  @RequiresApiKey
  @GetMapping("/users")
  public Page<User> findUsers(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return service.findAll(page, size);
  }

  @GetMapping("/users/{email}")
  public User findUser(@PathVariable @Email String email) {
    return service.findByEmail(email);
  }

  @PostMapping("/sign-up")
  public User signUp(@RequestBody @Valid @NotNull UserCreationRequest user) {
    return service.create(user);
  }
}
