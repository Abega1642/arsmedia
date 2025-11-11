package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.exception.JwtProcessingException;
import dev.razafindratelo.arsmedia.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.lang.Collections;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {
  private static final String TOKEN_TYPE_CLAIM = "token_type";
  private static final String USER_ID_CLAIM = "user_id";
  private static final String ROLES_CLAIM = "roles";
  private final SecretKey secretKey;
  private final JwtParser jwtParser;

  public JwtUtil(@Value("${app.jwt.secret}") String secretKey) {
    if (secretKey == null || secretKey.trim().isEmpty())
      throw new IllegalStateException("JWT secret key must be configured");

    log.info("JwtUtil initialized successfully");

    this.secretKey = Jwts.SIG.HS256.key().build();
    this.jwtParser = Jwts.parser().verifyWith(this.secretKey).build();
  }

  public String generateToken(User user) {
    return generateToken(user, Collections.emptyMap());
  }

  public String createToken(Map<String, Object> claims, String subject, Duration duration) {
    return buildToken(claims, subject, duration);
  }

  public String generateToken(User user, Map<String, Object> additionalClaims) {
    Map<String, Object> claims = buildBaseClaims(user);

    if (additionalClaims != null) {
      claims.putAll(additionalClaims);
    }

    return buildToken(claims, user.getEmail(), Duration.ofDays(2));
  }

  public boolean validateToken(String token) {
    if (StringUtils.isBlank(token)) {
      return false;
    }

    try {
      jwtParser.parseSignedClaims(token);
      return true;
    } catch (JwtException ex) {
      log.warn("JWT validation failed: {}", ex.getMessage());
      return false;
    }
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public String extractUserId(String token) {
    return extractAllClaims(token).get(USER_ID_CLAIM, String.class);
  }

  public String extractTokenType(String token) {
    return extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
  }

  public LocalDateTime extractExpiration(String token) {
    return extractAllClaims(token)
        .getExpiration()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(String token) {
    return extractAllClaims(token).get(ROLES_CLAIM, List.class);
  }

  public boolean isTokenExpired(String token) {
    try {
      LocalDateTime expiration = extractExpiration(token);
      return expiration.isBefore(LocalDateTime.now());
    } catch (JwtException ex) {
      log.warn("Failed to check token expiration: {}", ex.getMessage());
      return true;
    }
  }

  private String buildToken(Map<String, Object> claims, String subject, Duration duration) {
    validateTokenGenerationParameters(subject, duration);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date())
        .expiration(
            Date.from(
                LocalDateTime.now().plus(duration).atZone(ZoneId.systemDefault()).toInstant()))
        .signWith(secretKey)
        .compact();
  }

  Claims extractAllClaims(String token) {
    try {
      return jwtParser.parseSignedClaims(token).getPayload();
    } catch (JwtException ex) {
      log.warn("Failed to parse JWT claims: {}", ex.getMessage());
      throw new JwtProcessingException("Failed to parse JWT claims", ex);
    }
  }

  private Map<String, Object> buildBaseClaims(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(USER_ID_CLAIM, user.getId());
    claims.put("email", user.getEmail());
    claims.put("pseudo", user.getPseudo());
    claims.put("request_time", LocalDateTime.now().toString());

    List<String> roles =
        user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    claims.put(ROLES_CLAIM, roles);

    return claims;
  }

  private void validateTokenGenerationParameters(String subject, @NotNull Duration duration) {
    if (subject == null || subject.trim().isEmpty())
      throw new IllegalArgumentException("JWT subject cannot be null or empty");

    if (duration == null || duration.isNegative() || duration.isZero())
      throw new IllegalArgumentException("JWT duration must be positive");
  }
}
