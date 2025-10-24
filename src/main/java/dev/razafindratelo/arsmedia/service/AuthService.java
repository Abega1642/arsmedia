package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.UserMapper.toRUser;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.LoginRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {
  private final AuthenticationManager authenticationManager;
  private final UserService userService;

  public LoginResponse logIn(@NotNull @Valid LoginRequest request) {
    var authTime = LocalDateTime.now();
    log.info("Authentication attempt by user {}, at {}", request.email(), authTime);

    Authentication auth =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    if (!auth.isAuthenticated())
      throw new AuthorizationDeniedException("Authentication failed for email " + request.email());

    var user = userService.findByEmail(request.email());

    log.info("User with email {} authenticated at {}", request.email(), authTime);

    return new LoginResponse("SUCCESS", request.email(), authTime, toRUser(user));
  }
}
