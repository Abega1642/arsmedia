package dev.razafindratelo.arsmedia.config;

import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.service.ApiClientSecretService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@AllArgsConstructor
@Slf4j
public class ApiClientSecretFilter extends OncePerRequestFilter {

  private static final String LOCAL_HOST_IP = "127.0.0.1";
  private static final String[] PUBLIC_PATHS = {
    "/ping", "/", "/health/**", "/swagger-ui/**", "/v3/api-docs/**", "/doc/**", "/actuator/**"
  };

  private final ApiClientSecretService clientService;
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

    String path = request.getRequestURI();

    for (String pattern : PUBLIC_PATHS) {
      if (pathMatcher.match(pattern, path)) {
        filterChain.doFilter(request, response);
        return;
      }
    }

    String remoteAddr = request.getRemoteAddr();
    String host = request.getHeader("Host");

    if (isLocalRequest(remoteAddr, host)) {
      log.debug("Bypassing client secret check for local request: {}", forJava(host));
      filterChain.doFilter(request, response);
      return;
    }

    String clientId = request.getHeader("X-CLIENT-ID");
    String clientSecret = request.getHeader("X-CLIENT-SECRET");

    if (!clientService.isValid(clientId, clientSecret)) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.getWriter().write("Invalid or missing client credentials");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isLocalRequest(String remoteAddr, String host) {
    return LOCAL_HOST_IP.equals(remoteAddr)
        || "0:0:0:0:0:0:0:1".equals(remoteAddr)
        || (host != null && (host.startsWith("localhost") || host.startsWith(LOCAL_HOST_IP)));
  }
}
