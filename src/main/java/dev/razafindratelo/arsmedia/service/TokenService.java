package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.TokenMapper.toRest;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RTokenPair;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.TokenPairRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.token.TokenValidationResult;
import dev.razafindratelo.arsmedia.exception.InvalidTokenException;
import dev.razafindratelo.arsmedia.exception.TokenGenerationException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.mapper.TokenMapper;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.token.Token;
import dev.razafindratelo.arsmedia.model.token.TokenPair;
import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.TokenRepository;
import dev.razafindratelo.arsmedia.repository.model.token.JToken;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@AllArgsConstructor
@Slf4j
@Profile("prod")
@Validated
@Transactional
public class TokenService {

  private final UserService userService;
  private final TokenRepository tokenRepository;
  private final JwtUtil jwtUtil;

  @Value("${app.token.access-token-duration}")
  private Duration accessTokenDuration;

  @Value("${app.token.refresh-token-duration}")
  private Duration refreshTokenDuration;

  @Value("${app.token.max-active-tokens-per-user}")
  private int maxActiveTokensPerUser;

  @Value("${app.token.max-token-generation-retries:3}")
  private int maxTokenGenerationRetries;

  public RTokenPair generateTokenPair(@NotNull TokenPairRequest request) {
    var user = userService.findById(request.userId());

    if (!request.userEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException("Invalid user email");
    }

    var token = generateTokenPair(request.userEmail());

    return new RTokenPair(
        toRest(token.accessToken()),
        toRest(token.refreshToken()),
        token.requestTime());
  }

  public RTokenPair regenerateTokenPair(@NotNull @NotBlank String refreshTokenValue) {
    var tokenPair = refreshTokenPair(refreshTokenValue);
    return new RTokenPair(
        toRest(tokenPair.accessToken()),
        toRest(tokenPair.refreshToken()),
        tokenPair.requestTime());
  }

  public TokenPair generateTokenPair(@Email String userEmail) {
    log.info("Generating token pair for user: {}", forJava(userEmail));

    var user = validateUserForTokenGeneration(userEmail);
    enforceTokenQuota(userEmail);

    var accessToken = generateAccessToken(user);
    var refreshToken = generateRefreshToken(user);

    return new TokenPair(accessToken, refreshToken, LocalDateTime.now());
  }

  public TokenValidationResult validateToken(String tokenValue, TokenType expectedType) {
    if (StringUtils.isBlank(tokenValue)) {
      return TokenValidationResult.invalid("Token is null or empty");
    }

    if (!jwtUtil.validateToken(tokenValue)) {
      return TokenValidationResult.invalid("JWT signature invalid");
    }

    return tokenRepository
        .findByValueAndIsValid(tokenValue, true)
        .map(token -> validateTokenAttributes(token, expectedType))
        .orElse(TokenValidationResult.invalid("Token not found in database"));
  }

  /**
   * Stub implementation to allow application startup without DB dependency
   */
  public Object findTokenByValue(String tokenValue) {
    // Stub implementation to allow startup without DB
    return null;
  }

  public TokenPair refreshTokenPair(String refreshTokenValue) {
    var validationResult = validateToken(refreshTokenValue, TokenType.REFRESH_TOKEN);

    if (!validationResult.valid()) {
      throw new InvalidTokenException(
          "Invalid refresh token: " + validationResult.reason());
    }

    safelyInvalidateToken(refreshTokenValue);
    return generateTokenPair(validationResult.userEmail());
  }

  @Scheduled(cron = "${app.token.cleanup-cron}")
  public void cleanupExpiredTokens() {
    var now = LocalDateTime.now();
    var expiredTokens = tokenRepository.findByIsValidAndExpirationBefore(true, now);

    expiredTokens.forEach(token -> token.setValid(false));
    tokenRepository.saveAll(expiredTokens);
  }

  private User validateUserForTokenGeneration(String userEmail) {
    var user = userService.findByEmail(userEmail);

    if (!user.isActivated()) {
      throw new UserNotActivatedException(
          "User account is not activated: " + forJava(userEmail));
    }
    return user;
  }

  private Token generateAccessToken(User user) {
    return generateToken(
        user,
        TokenType.ACCESS_TOKEN,
        accessTokenDuration,
        () -> jwtUtil.generateToken(user));
  }

  private Token generateRefreshToken(User user) {
    return generateToken(
        user,
        TokenType.REFRESH_TOKEN,
        refreshTokenDuration,
        () ->
            jwtUtil.createToken(
                Map.of("token_type", "REFRESH"),
                user.getEmail(),
                refreshTokenDuration));
  }

  private Token generateToken(
      User user, TokenType type, Duration duration, TokenValueGenerator generator) {

    for (int attempt = 1; attempt <= maxTokenGenerationRetries; attempt++) {
      try {
        var value = generator.generate();
        return persistToken(user, value, type, duration);
      } catch (Exception e) {
        if (attempt == maxTokenGenerationRetries) {
          throw new TokenGenerationException("Token generation failed", e);
        }
      }
    }
    throw new TokenGenerationException("Token generation failed");
  }

  private Token persistToken(User user, String value, TokenType type, Duration duration) {
    var now = LocalDateTime.now();

    var jToken =
        JToken.builder()
            .id(randomUUID().toString())
            .user(UserMapper.toJUser(user))
            .value(value)
            .type(type)
            .creation(now)
            .expiration(now.plus(duration))
            .isValid(true)
            .build();

    try {
      return TokenMapper.toModel(tokenRepository.save(jToken));
    } catch (DataAccessException e) {
      throw new TokenGenerationException("Failed to persist token", e);
    }
  }

  private void enforceTokenQuota(String userEmail) {
    var count = tokenRepository.countByUserEmailAndIsValid(userEmail, true);

    if (count >= maxActiveTokensPerUser) {
      var tokens =
          tokenRepository.findByUserEmailAndIsValidOrderByCreationAsc(userEmail, true);

      tokens.stream()
          .limit(count - maxActiveTokensPerUser + 1)
          .forEach(t -> t.setValid(false));

      tokenRepository.saveAll(tokens);
    }
  }

  private TokenValidationResult validateTokenAttributes(JToken token, TokenType expectedType) {
    if (token.getType() != expectedType) {
      return TokenValidationResult.invalid("Token type mismatch");
    }

    if (token.getExpiration().isBefore(LocalDateTime.now())) {
      safelyInvalidateToken(token.getValue());
      return TokenValidationResult.invalid("Token expired");
    }

    return TokenValidationResult.valid(
        token.getUser().getEmail(), token.getType());
  }

  private void safelyInvalidateToken(String value) {
    tokenRepository.findByValueAndIsValid(value, true).ifPresent(t -> {
      t.setValid(false);
      tokenRepository.save(t);
    });
  }

  @FunctionalInterface
  private interface TokenValueGenerator {
    String generate();
  }
}
