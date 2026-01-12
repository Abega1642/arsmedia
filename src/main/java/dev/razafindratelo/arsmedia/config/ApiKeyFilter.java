package dev.razafindratelo.arsmedia.config;

import org.springframework.context.annotation.Profile;
import dev.razafindratelo.arsmedia.service.ApiKeyService;
import dev.razafindratelo.arsmedia.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API Key authentication filter.
 *
 * IMPORTANT:
 * - This filter is ONLY active when the "prod" profile is enabled.
 * - In default / CI / Render free tier, this filter is NOT loaded.
 * - Prevents DB-backed beans from crashing application startup.
 */
@Component
@AllArgsConstructor
@Slf4j
@Profile("prod")
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final String[] SECURE_PATHS = { "/users" };

  private final @Lazy ApiKeyService service;
  private final @Lazy UserService userService;

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Override
  protected void doFilterInternal(
      @Nullable HttpServletRequest request,
      @Nullable HttpServletResponse response,
      @Nullable FilterChain filterChain)
      throws ServletException, IOException {

    if (request == null || response == null || filterChain == null) {
      log.warn("Received null request, response, or filter chain");
      return;
    }

    if (requiresApiKey(request)) {
      String apiKey = request.getHeader("X-API-KEY");

      if (!isValidApiKey(apiKey)) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Valid API key required");
        return;
      }

      setupAuthentication(apiKey);
    }

    filterChain.doFilter(request, response);
  }

  private void setupAuthentication(String apiKey) {
    try {
      var apiKeyEntity = service.findByAPIKeyValue(apiKey);
      var userEmail = apiKeyEntity.owner().getEmail();

      var user = userService.loadUserByUsername(userEmail);

      var authentication =
          new UsernamePasswordAuthenticationToken(
              user,
              null,
              user.getAuthorities()
          );

      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (Exception e) {
      log.warn("Failed to set up authentication for API key", e);
    }
  }

  private boolean requiresApiKey(HttpServletRequest request) {
    String path = request.getRequestURI();
    for (String pattern : SECURE_PATHS) {
      if (pathMatcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private boolean isValidApiKey(String apiKey) {
    return service.isApiKeyValid(apiKey);
  }
}
