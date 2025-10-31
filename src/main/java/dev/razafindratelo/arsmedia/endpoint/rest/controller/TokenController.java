package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RTokenPair;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.TokenPairRequest;
import dev.razafindratelo.arsmedia.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@AllArgsConstructor
@RequestMapping("/auth/token")
public class TokenController {
  private final TokenService service;

  @PostMapping("/token-pairs")
  public RTokenPair generateTokenPair(
      @RequestBody @Valid @NotNull TokenPairRequest tokenPairRequest) {
    return service.generateTokenPair(tokenPairRequest);
  }

  @PostMapping("/refresh-token-pairs")
  public RTokenPair refreshTokenPair(
      @RequestParam(name = "refresh_token") @NotBlank @NotNull String refreshToken) {
    return service.regenerateTokenPair(refreshToken);
  }
}
