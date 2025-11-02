package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeResponse;
import dev.razafindratelo.arsmedia.service.AuthCodeService;
import jakarta.mail.internet.AddressException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthCodeController {
  private final AuthCodeService service;

  @PostMapping("/auth-code/{userId}")
  public AuthCodeResponse sendAuthCodeTo(@PathVariable @NotNull String userId)
      throws AddressException {
    return service.sentAuthCodeTo(userId);
  }

  @PatchMapping("/activate-profile/{userId}")
  public boolean activateUserProfile(
      @PathVariable @NotNull String userId, @RequestBody @Valid AuthCodeRequest authCodeRequest) {
    return service.activateUserProfile(userId, authCodeRequest);
  }
}
