package dev.razafindratelo.arsmedia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ErrorResponse;
import dev.razafindratelo.arsmedia.exception.*;
import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.service.TokenService;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

  private static final List<String> SECURED_PATHS =
      List.of(
          "/users/**", "/auth/api-keys/**", "/api/media/video/upload/**", "/api/media/videos/**");
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int MIN_TOKEN_LENGTH = 10;

  private final TokenService tokenService;
  private final UserService userService;
  private final ObjectMapper om;

  @Override
  protected void doFilterInternal(
      @Nullable HttpServletRequest request,
      @Nullable HttpServletResponse response,
      @Nullable FilterChain filterChain)
      throws ServletException, IOException {

    try {
      if (request == null || response == null || filterChain == null) {
        log.warn("Request, response or filter chain is null");
        return;
      }

      if (!shouldFilter(request)) {
        filterChain.doFilter(request, response);
        return;
      }

      var token = extractTokenFromRequest(request);
      validateToken(token);

      var authentication = createAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      log.debug("Successfully authenticated user for path: {}", request.getServletPath());
      filterChain.doFilter(request, response);

    } catch (AuthenticationException ex) {
      handleAuthenticationException(ex, response, request);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private boolean shouldFilter(HttpServletRequest request) {
    var path = request.getServletPath();
    var method = request.getMethod();

    if ("OPTIONS".equalsIgnoreCase(method)) {
      return false;
    }

    return SECURED_PATHS.stream().anyMatch(path::startsWith);
  }

  private String extractTokenFromRequest(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");

    if (authHeader == null || authHeader.isBlank())
      throw new MissingAuthorizationException("Authorization header is required");

    if (!authHeader.startsWith(BEARER_PREFIX))
      throw new InvalidAuthorizationFormatException(
          "Authorization header must start with 'Bearer '");

    var token = authHeader.substring(BEARER_PREFIX.length()).trim();

    if (token.length() < MIN_TOKEN_LENGTH) throw new InvalidTokenException("Token is too short");

    return token;
  }

  private void validateToken(String tokenValue) {
    var validationResult = tokenService.validateToken(tokenValue, TokenType.ACCESS_TOKEN);

    if (!validationResult.valid()) {
      throw new InvalidTokenException("Token validation failed: " + validationResult.reason());
    }

    log.debug("Token validated successfully for user: {}", validationResult.userEmail());
  }

  private Authentication createAuthentication(String tokenValue) {
    var token = tokenService.findTokenByValue(tokenValue);

    var user = userService.findByEmail(token.user().getEmail());

    if (!user.isActivated()) {
      throw new UserNotActivatedException("User account is deactivated");
    }

    return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
  }

  private void handleAuthenticationException(
      AuthenticationException ex,
      @Nullable HttpServletResponse response,
      @Nullable HttpServletRequest request)
      throws IOException {

    if (response == null) {
      log.error("Response is null, cannot send error response");
      return;
    }

    log.warn("Authentication failed: {}", ex.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    var path = request != null ? request.getServletPath() : "N/A";
    var errorResponse = ErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), path);

    try (var writer = response.getWriter()) {
      writer.write(om.writeValueAsString(errorResponse));
    }
  }

  @Override
  protected boolean shouldNotFilter(@Nullable HttpServletRequest request) {
    if (request == null) return true;

    var path = request.getServletPath();
    return SECURED_PATHS.stream().noneMatch(path::startsWith);
  }
}
