package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.AuthCodeResponse;
import dev.razafindratelo.arsmedia.service.AuthCodeService;
import jakarta.mail.internet.AddressException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthCodeController {
  private final AuthCodeService service;

  @PostMapping("/auth/auth-code/{userId}")
  public AuthCodeResponse sendAuthCodeTo(@PathVariable String userId) throws AddressException {
    return service.sentAuthCodeTo(userId);
  }
}
