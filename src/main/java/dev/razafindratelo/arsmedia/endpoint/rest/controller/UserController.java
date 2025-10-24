package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.config.RequiresApiKey;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Validated
public class UserController {
  private final UserService service;

  @PreAuthorize("hasRole('ADMIN')")
  @RequiresApiKey
  @GetMapping()
  public Page<User> findUsers(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return service.findAll(page, size);
  }

  @GetMapping("/users/{email}")
  public User findUser(@PathVariable @Email String email) {
    return service.findByEmail(email);
  }

  @PostMapping("/users/sign-up")
  public User signUp(@RequestBody @Valid @NotNull RUser user) {
    return service.create(user);
  }
}
