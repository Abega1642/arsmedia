package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
import dev.razafindratelo.arsmedia.exception.InvalidTokenException;
import dev.razafindratelo.arsmedia.exception.RessourceNotFoundException;
import dev.razafindratelo.arsmedia.exception.TokenNotFoundException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.model.token.Token;
import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.TokenRepository;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Slf4j
class TokenServiceIT extends FacadeIT {

  protected final String test_email = "test-email@example.com";
  @Autowired private TokenService tokenService;
  @Autowired private UserRepository userRepository;
  @Autowired private TokenRepository tokenRepository;
  @Autowired private UserService userService;
  @Autowired private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    tokenRepository.deleteAll();
    userRepository.deleteByEmail(test_email);

    RUser testRUser =
        new RUser(test_email, "+1234567890", "testuser", UserRole.USER, "encodedPassword123");

    userService.create(testRUser);
    userService.updateActivationStatusByEmail(test_email, true);
  }

  @AfterEach
  void tearDown() {
    tokenRepository.deleteAll();
    userRepository.deleteByEmail(test_email);
  }

  @Test
  void should_generate_token_pair_successfully() {
    var tokenPair = tokenService.generateTokenPair(test_email);

    assertNotNull(tokenPair);
    assertNotNull(tokenPair.accessToken());
    assertNotNull(tokenPair.refreshToken());
    assertEquals(test_email, tokenPair.accessToken().user().getEmail());
    assertEquals(test_email, tokenPair.refreshToken().user().getEmail());

    var savedTokens = tokenRepository.findAll();
    assertEquals(2, savedTokens.size());

    var accessToken =
        savedTokens.stream().filter(token -> token.getType() == TokenType.ACCESS_TOKEN).findFirst();
    assertTrue(accessToken.isPresent());
    assertTrue(accessToken.get().isValid());
    assertNotNull(accessToken.get().getUser());

    var refreshToken =
        savedTokens.stream()
            .filter(token -> token.getType() == TokenType.REFRESH_TOKEN)
            .findFirst();
    assertTrue(refreshToken.isPresent());
    assertTrue(refreshToken.get().isValid());
    assertNotNull(refreshToken.get().getUser());
  }

  @Test
  void should_generate_access_token_successfully() {
    var accessToken = tokenService.generateAccessToken(test_email);

    assertNotNull(accessToken);
    assertEquals(TokenType.ACCESS_TOKEN, accessToken.type());
    assertEquals(test_email, accessToken.user().getEmail());
    assertTrue(accessToken.expiration().isAfter(LocalDateTime.now()));
    assertTrue(accessToken.isValid());

    var savedToken = tokenRepository.findByValue(accessToken.value());
    assertTrue(savedToken.isPresent());
    assertNotNull(savedToken.get().getUser());
    assertEquals(test_email, savedToken.get().getUser().getEmail());
  }

  @Test
  void should_generate_refresh_token_successfully() {
    var refreshToken = tokenService.generateRefreshToken(test_email);

    assertNotNull(refreshToken);
    assertEquals(TokenType.REFRESH_TOKEN, refreshToken.type());
    assertEquals(test_email, refreshToken.user().getEmail());
    assertTrue(refreshToken.expiration().isAfter(LocalDateTime.now()));
    assertTrue(refreshToken.isValid());

    var savedToken = tokenRepository.findByValue(refreshToken.value());
    assertTrue(savedToken.isPresent());
    assertNotNull(savedToken.get().getUser());
    assertEquals(test_email, savedToken.get().getUser().getEmail());
  }

  @Test
  void should_validate_valid_token_successfully() {
    var tokenPair = tokenService.generateTokenPair(test_email);
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    var validationResult = tokenService.validateToken(accessTokenValue, TokenType.ACCESS_TOKEN);

    assertTrue(validationResult.valid());
    assertEquals(test_email, validationResult.userEmail());
  }

  @Test
  void should_return_invalid_for_expired_token() {
    var tokenPair = tokenService.generateTokenPair("test@example.com");
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    var expiredJToken = tokenRepository.findByValue(accessTokenValue).orElseThrow();
    expiredJToken.setExpiration(LocalDateTime.now().minusHours(1));
    tokenRepository.save(expiredJToken);

    var validationResult = tokenService.validateToken(accessTokenValue, TokenType.ACCESS_TOKEN);

    assertFalse(validationResult.valid());
    assertEquals("Token expired", validationResult.reason());

    var invalidatedToken = tokenRepository.findByValue(accessTokenValue);
    assertTrue(invalidatedToken.isPresent());
    assertFalse(invalidatedToken.get().isValid());
  }

  @Test
  void should_return_invalid_for_wrong_token_type() {
    var tokenPair = tokenService.generateTokenPair(test_email);
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    var validationResult = tokenService.validateToken(accessTokenValue, TokenType.REFRESH_TOKEN);

    assertFalse(validationResult.valid());
    assertTrue(validationResult.reason().contains("Token type mismatch"));
  }

  @Test
  void should_refresh_token_pair_successfully() {
    var originalTokenPair = tokenService.generateTokenPair(test_email);
    var refreshTokenValue = originalTokenPair.refreshToken().value();

    when(jwtUtil.validateToken(refreshTokenValue)).thenReturn(true);
    when(jwtUtil.extractUsername(refreshTokenValue)).thenReturn(test_email);

    var newTokenPair = tokenService.refreshTokenPair(refreshTokenValue);

    assertNotNull(newTokenPair);
    assertNotEquals(originalTokenPair.accessToken().value(), newTokenPair.accessToken().value());

    var oldRefreshToken = tokenRepository.findByValue(refreshTokenValue);
    assertTrue(oldRefreshToken.isPresent());
    assertFalse(oldRefreshToken.get().isValid());
  }

  @Test
  void should_throw_exception_when_refreshing_invalid_token() {
    when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

    assertThrows(InvalidTokenException.class, () -> tokenService.refreshTokenPair("invalid-token"));
  }

  @Test
  void should_revoke_token_successfully() {
    var tokenPair = tokenService.generateTokenPair(test_email);
    var tokenValue = tokenPair.accessToken().value();

    tokenService.revokeToken(tokenValue, test_email);

    var revokedToken = tokenRepository.findByValue(tokenValue);
    assertTrue(revokedToken.isPresent());
    assertFalse(revokedToken.get().isValid());
  }

  @Test
  void should_throw_exception_when_revoking_nonexistent_token() {
    assertThrows(
        TokenNotFoundException.class,
        () -> tokenService.revokeToken("nonexistent-token", test_email));
  }

  @Test
  void should_revoke_all_user_tokens_successfully() {
    tokenService.generateTokenPair(test_email);
    tokenService.generateAccessToken(test_email);

    var activeTokensBefore = tokenRepository.findByUserEmailAndIsValid(test_email, true);
    assertEquals(3, activeTokensBefore.size());

    tokenService.revokeAllUserTokens(test_email);

    var activeTokensAfter = tokenRepository.findByUserEmailAndIsValid(test_email, true);
    assertTrue(activeTokensAfter.isEmpty());
  }

  @Test
  void should_revoke_tokens_by_type_successfully() {
    tokenService.generateTokenPair(test_email);
    tokenService.generateAccessToken(test_email);

    var accessTokensBefore =
        tokenRepository.findByUserEmailAndTypeAndIsValid(test_email, TokenType.ACCESS_TOKEN, true);
    assertEquals(2, accessTokensBefore.size());

    tokenService.revokeUserTokensByType(test_email, TokenType.ACCESS_TOKEN);

    var accessTokensAfter =
        tokenRepository.findByUserEmailAndTypeAndIsValid(test_email, TokenType.ACCESS_TOKEN, true);
    assertTrue(accessTokensAfter.isEmpty());

    var refreshTokensAfter =
        tokenRepository.findByUserEmailAndTypeAndIsValid(test_email, TokenType.REFRESH_TOKEN, true);
    assertEquals(1, refreshTokensAfter.size());
  }

  @Test
  void should_enforce_token_quota_by_invalidating_oldest_tokens() {
    for (int i = 0; i < 5; i++) {
      tokenService.generateAccessToken(test_email);
    }

    var activeTokens = tokenRepository.findByUserEmailAndIsValid(test_email, true);
    assertEquals(3, activeTokens.size());

    var invalidTokens = tokenRepository.findByUserEmailAndIsValid(test_email, false);
    assertEquals(2, invalidTokens.size());
  }

  @Test
  void should_cleanup_expired_tokens_successfully() {
    var tokenPair = tokenService.generateTokenPair(test_email);

    var expiredJToken = tokenRepository.findByValue(tokenPair.accessToken().value()).orElseThrow();
    expiredJToken.setExpiration(LocalDateTime.now().minusHours(1));
    tokenRepository.save(expiredJToken);

    var expiredTokensBefore =
        tokenRepository.findByIsValidAndExpirationBefore(true, LocalDateTime.now());
    assertEquals(1, expiredTokensBefore.size());

    tokenService.cleanupExpiredTokens();

    var expiredTokensAfter =
        tokenRepository.findByIsValidAndExpirationBefore(true, LocalDateTime.now());
    assertTrue(expiredTokensAfter.isEmpty());

    var cleanedToken = tokenRepository.findByValue(tokenPair.accessToken().value());
    assertTrue(cleanedToken.isPresent());
    assertFalse(cleanedToken.get().isValid());
  }

  @Test
  void should_get_active_user_tokens_successfully() {
    tokenService.generateTokenPair(test_email);

    var activeTokens = tokenService.getActiveUserTokens(test_email);

    assertEquals(2, activeTokens.size());
    assertTrue(activeTokens.stream().allMatch(Token::isValid));
    assertTrue(activeTokens.stream().allMatch(token -> token.user().getEmail().equals(test_email)));
  }

  @Test
  void should_get_user_tokens_by_type_successfully() {
    tokenService.generateTokenPair(test_email);

    var accessTokens = tokenService.getUserTokensByType(test_email, TokenType.ACCESS_TOKEN);

    assertEquals(1, accessTokens.size());
    assertEquals(TokenType.ACCESS_TOKEN, accessTokens.getFirst().type());
    assertEquals(test_email, accessTokens.getFirst().user().getEmail());
  }

  @Test
  void should_find_token_by_value_successfully() {
    var tokenPair = tokenService.generateTokenPair(test_email);
    var tokenValue = tokenPair.accessToken().value();

    var foundToken = tokenService.findTokenByValue(tokenValue);

    assertNotNull(foundToken);
    assertEquals(tokenValue, foundToken.value());
    assertEquals(test_email, foundToken.user().getEmail());
  }

  @Test
  void should_throw_exception_when_finding_nonexistent_token() {
    assertThrows(
        RessourceNotFoundException.class, () -> tokenService.findTokenByValue("nonexistent-token"));
  }

  @Test
  void should_check_token_validity_correctly() {
    var tokenPair = tokenService.generateTokenPair(test_email);
    var tokenValue = tokenPair.accessToken().value();

    var isValid = tokenService.isTokenValid(tokenValue);

    assertTrue(isValid);
  }

  @Test
  void should_return_false_for_invalid_token() {
    var isValid = tokenService.isTokenValid("invalid-token");

    assertFalse(isValid);
  }

  @Test
  void should_throw_exception_for_inactive_user() {
    var inactiveUser =
        new RUser(
            "inactive@example.com",
            "+1234567891",
            "inactiveuser",
            UserRole.USER,
            "encodedPassword456");
    userService.create(inactiveUser);

    var jUser = userRepository.findByEmail("inactive@example.com").orElseThrow();
    jUser.setActivated(false);
    userRepository.save(jUser);

    assertThrows(
        UserNotActivatedException.class,
        () -> tokenService.generateTokenPair("inactive@example.com"));
  }

  @Test
  void should_throw_exception_for_nonexistent_user() {
    assertThrows(
        RessourceNotFoundException.class,
        () -> tokenService.generateTokenPair("nonexistent@example.com"));
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
      var jwtUtil = mock(JwtUtil.class);

      when(jwtUtil.generateToken(any(User.class)))
          .thenAnswer(
              invocation -> {
                var user = invocation.getArgument(0, User.class);
                return "mock-access-token-" + user.getEmail() + "-" + UUID.randomUUID();
              });

      when(jwtUtil.createToken(anyMap(), anyString(), any(Duration.class)))
          .thenAnswer(
              invocation -> {
                var subject = invocation.getArgument(1, String.class);
                return "mock-refresh-token-" + subject + "-" + UUID.randomUUID();
              });

      when(jwtUtil.validateToken(anyString())).thenReturn(true);
      when(jwtUtil.extractUsername(anyString())).thenReturn("test-email@example.com");

      return jwtUtil;
    }
  }
}
