package dev.razafindratelo.arsmedia.config;

import dev.razafindratelo.arsmedia.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@Component
@AllArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {
  private final ApiKeyService service;

  @Override
  protected void doFilterInternal(
      @Nullable HttpServletRequest request,
      @Nullable HttpServletResponse response,
      @Nullable FilterChain filterChain)
      throws ServletException, IOException {

    if (request == null || response == null || filterChain == null) {
      logger.warn("Received null request, response, or filter chain");
      return;
    }
    if (requiresApiKey(request)) {
      String apiKey = request.getHeader("X-API-KEY");
      if (!isValidApiKey(apiKey)) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Valid API key required");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean requiresApiKey(HttpServletRequest request) {
    var handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

    if (handler instanceof HandlerMethod handlerMethod) {
      return handlerMethod.getMethodAnnotation(RequiresApiKey.class) != null
          || handlerMethod.getBeanType().getAnnotation(RequiresApiKey.class) != null;
    }

    return false;
  }

  private boolean isValidApiKey(String apiKey) {
    return service.isApiKeyValid(apiKey);
  }
}
