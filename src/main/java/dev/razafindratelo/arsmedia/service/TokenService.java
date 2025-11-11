package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.TokenMapper.toRest;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RTokenPair;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.TokenPairRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.token.TokenValidationResult;
import dev.razafindratelo.arsmedia.exception.*;
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
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Slf4j
@Transactional
@RequiredArgsConstructor
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

    if (!request.userEmail().equals(user.getEmail()))
      throw new IllegalArgumentException("Invalid user email");
    var token = generateTokenPair(request.userEmail());

    return new RTokenPair(
        toRest(token.accessToken()), toRest(token.refreshToken()), token.requestTime());
  }

  public RTokenPair regenerateTokenPair(@NotNull @NotBlank String refreshTokenValue) {
    var tokenPair = refreshTokenPair(refreshTokenValue);
    return new RTokenPair(
        toRest(tokenPair.accessToken()), toRest(tokenPair.refreshToken()), tokenPair.requestTime());
  }

  public TokenPair generateTokenPair(@Email String userEmail) {
    log.info("Generating token pair for user: {}", userEmail);

    var user = validateUserForTokenGeneration(userEmail);
    enforceTokenQuota(userEmail);

    var accessToken = generateAccessToken(user);
    var refreshToken = generateRefreshToken(user);

    log.info("Successfully generated token pair for user: {}", userEmail);
    return new TokenPair(accessToken, refreshToken, LocalDateTime.now());
  }

  public Token generateAccessToken(@Email String userEmail) {
    var user = validateUserForTokenGeneration(userEmail);
    return generateAccessToken(user);
  }

  public Token generateRefreshToken(@Email String userEmail) {
    var user = validateUserForTokenGeneration(userEmail);
    return generateRefreshToken(user);
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

  public TokenPair refreshTokenPair(String refreshTokenValue) {
    log.info("Refreshing token pair");

    var validationResult = validateToken(refreshTokenValue, TokenType.REFRESH_TOKEN);
    if (!validationResult.valid()) {
      throw new InvalidTokenException("Invalid refresh token: " + validationResult.reason());
    }

    safelyInvalidateToken(refreshTokenValue);
    return generateTokenPair(validationResult.userEmail());
  }

  public void revokeToken(@NotBlank @NotNull String tokenValue, @Email String userEmail) {
    log.info("Revoking token for user: {}", userEmail);

    var token =
        tokenRepository
            .findByValueAndUserEmailAndIsValid(tokenValue, userEmail, true)
            .orElseThrow(
                () -> new TokenNotFoundException("No active token found for user: " + userEmail));

    token.setValid(false);
    tokenRepository.save(token);
    log.info("Successfully revoked token for user: {}", userEmail);
  }

  public void revokeAllUserTokens(@Email @NotBlank @NotNull String userEmail) {
    log.info("Revoking all tokens for user: {}", userEmail);

    var activeTokens = tokenRepository.findByUserEmailAndIsValid(userEmail, true);
    if (activeTokens.isEmpty()) {
      log.info("No active tokens found for user: {}", userEmail);
      return;
    }

    activeTokens.forEach(token -> token.setValid(false));
    tokenRepository.saveAll(activeTokens);
    log.info("Successfully revoked {} tokens for user: {}", activeTokens.size(), userEmail);
  }

  public void revokeUserTokensByType(
      @Email @NotBlank @NotNull String userEmail, @NotNull TokenType type) {
    log.info("Revoking {} tokens for user: {}", type, userEmail);

    var tokens = tokenRepository.findByUserEmailAndTypeAndIsValid(userEmail, type, true);
    if (tokens.isEmpty()) {
      log.info("No active {} tokens found for user: {}", type, userEmail);
      return;
    }

    tokens.forEach(token -> token.setValid(false));
    tokenRepository.saveAll(tokens);
    log.info("Successfully revoked {} {} tokens for user: {}", tokens.size(), type, userEmail);
  }

  @Scheduled(cron = "${app.token.cleanup-cron}")
  @Transactional
  public void cleanupExpiredTokens() {
    log.info("Starting expired tokens cleanup");

    var now = LocalDateTime.now();
    var expiredTokens = tokenRepository.findByIsValidAndExpirationBefore(true, now);

    if (expiredTokens.isEmpty()) {
      log.info("No expired tokens found to clean up");
      return;
    }

    expiredTokens.forEach(token -> token.setValid(false));
    var invalidatedTokens = tokenRepository.saveAll(expiredTokens);
    log.info("Successfully cleaned up {} expired tokens", invalidatedTokens.size());
  }

  public List<Token> getActiveUserTokens(@Email String userEmail) {
    return tokenRepository.findByUserEmailAndIsValid(userEmail, true).stream()
        .map(TokenMapper::toModel)
        .collect(Collectors.toList());
  }

  public List<Token> getUserTokensByType(@Email String userEmail, TokenType type) {
    return tokenRepository.findByUserEmailAndTypeAndIsValid(userEmail, type, true).stream()
        .map(TokenMapper::toModel)
        .collect(Collectors.toList());
  }

  public Token findTokenByValue(String tokenValue) {
    return tokenRepository
        .findByValue(tokenValue)
        .map(TokenMapper::toModel)
        .orElseThrow(() -> new RessourceNotFoundException("No token found with the given value."));
  }

  public boolean isTokenValid(String tokenValue) {
    return tokenRepository
        .findByValueAndIsValid(tokenValue, true)
        .map(token -> token.getExpiration().isAfter(LocalDateTime.now()))
        .orElse(false);
  }

  private User validateUserForTokenGeneration(String userEmail) {
    var user = userService.findByEmail(userEmail);

    log.info(
        "Check user activity for API key generation. User : { email ={}, isActive= {} }",
        user.getEmail(),
        user.isActivated());

    if (!user.isActivated())
      throw new UserNotActivatedException("User account is not activated: " + userEmail);

    return user;
  }

  private Token generateAccessToken(User user) {
    return generateToken(
        user, TokenType.ACCESS_TOKEN, accessTokenDuration, () -> jwtUtil.generateToken(user));
  }

  private Token generateRefreshToken(User user) {
    return generateToken(
        user,
        TokenType.REFRESH_TOKEN,
        refreshTokenDuration,
        () -> {
          Map<String, Object> claims =
              Map.of(
                  "token_type",
                  "REFRESH",
                  "user_id",
                  user.getId(),
                  "creation",
                  LocalDateTime.now().toString());
          return jwtUtil.createToken(claims, user.getEmail(), refreshTokenDuration);
        });
  }

  private Token generateToken(
      User user, TokenType type, Duration duration, TokenValueGenerator valueGenerator) {
    for (var attempt = 1; attempt <= maxTokenGenerationRetries; attempt++) {
      try {
        var tokenValue = valueGenerator.generate();

        if (tokenRepository.existsByValueAndIsValid(tokenValue, true)) {
          log.warn(
              "Duplicate token of type {} generated, retrying... Attempt: {}/{}",
              type,
              attempt,
              maxTokenGenerationRetries);
          continue;
        }

        return persistToken(user, tokenValue, type, duration);

      } catch (Exception e) {
        handleTokenGenerationError(attempt, e);
      }
    }

    throw new TokenGenerationException(
        "Failed to generate unique " + type + " after " + maxTokenGenerationRetries + " attempts");
  }

  private Token persistToken(User user, String tokenValue, TokenType type, Duration duration) {
    var now = LocalDateTime.now();
    var expiration = now.plus(duration);

    var jToken =
        JToken.builder()
            .id(UUID.randomUUID().toString())
            .user(UserMapper.toJUser(user))
            .isValid(true)
            .value(tokenValue)
            .type(type)
            .creation(now)
            .expiration(expiration)
            .build();

    try {
      var savedToken = tokenRepository.save(jToken);
      log.debug("Persisted {} token for user: {}", type, user.getEmail());
      return TokenMapper.toModel(savedToken);

    } catch (DataAccessException e) {
      log.error("Failed to persist token for user: {}, error: {}", user.getEmail(), e.getMessage());
      throw new TokenGenerationException("Failed to save token to database", e);
    }
  }

  private void enforceTokenQuota(@Email String userEmail) {
    var activeTokenCount = tokenRepository.countByUserEmailAndIsValid(userEmail, true);

    if (activeTokenCount >= maxActiveTokensPerUser) {
      log.info(
          "Enforcing token quota for user: {} ({} active tokens)", userEmail, activeTokenCount);

      var oldestTokens =
          tokenRepository.findByUserEmailAndIsValidOrderByCreationAsc(userEmail, true);
      var tokensToInvalidate = (int) (activeTokenCount - maxActiveTokensPerUser + 1);

      var tokensToUpdate =
          oldestTokens.stream().limit(tokensToInvalidate).collect(Collectors.toList());

      for (var token : tokensToUpdate) {
        token.setValid(false);
        log.debug("Invalidating old token for user: {}", userEmail);
      }

      if (!tokensToUpdate.isEmpty()) {
        tokenRepository.saveAll(tokensToUpdate);
        log.info("Invalidated {} oldest tokens for user: {}", tokensToUpdate.size(), userEmail);
      }
    }
  }

  private TokenValidationResult validateTokenAttributes(JToken token, TokenType expectedType) {
    if (token.getType() != expectedType) {
      return TokenValidationResult.invalid(
          "Token type mismatch. Expected: " + expectedType + ", Actual: " + token.getType(),
          token.getType());
    }

    if (token.getExpiration().isBefore(LocalDateTime.now())) {
      safelyInvalidateToken(token.getValue());
      return TokenValidationResult.invalid("Token expired");
    }

    if (!token.getUser().isActivated()) {
      safelyInvalidateToken(token.getValue());
      return TokenValidationResult.invalid("User account inactive");
    }

    return TokenValidationResult.valid(token.getUser().getEmail(), token.getType());
  }

  private void safelyInvalidateToken(String tokenValue) {
    try {
      tokenRepository
          .findByValueAndIsValid(tokenValue, true)
          .ifPresent(
              token -> {
                token.setValid(false);
                tokenRepository.save(token);
                log.debug("Invalidated token: {}", maskToken(tokenValue));
              });
    } catch (Exception e) {
      log.error("Failed to invalidate token: {}, error: {}", maskToken(tokenValue), e.getMessage());
    }
  }

  private void handleTokenGenerationError(int attempt, Exception e) {
    log.error(
        "Token generation failed on attempt {}/{}: {}",
        attempt,
        maxTokenGenerationRetries,
        e.getMessage());

    if (attempt == maxTokenGenerationRetries) {
      throw new TokenGenerationException(
          "Token generation failed after " + maxTokenGenerationRetries + " attempts", e);
    }
  }

  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "***";
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }

  @FunctionalInterface
  private interface TokenValueGenerator {
    String generate() throws TokenGenerationException;
  }
}
