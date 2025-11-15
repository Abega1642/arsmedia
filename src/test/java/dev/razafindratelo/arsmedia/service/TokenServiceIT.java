package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.token.TokenValidationResult;
import dev.razafindratelo.arsmedia.exception.InvalidTokenException;
import dev.razafindratelo.arsmedia.exception.RessourceNotFoundException;
import dev.razafindratelo.arsmedia.exception.TokenNotFoundException;
import dev.razafindratelo.arsmedia.exception.UserNotActivatedException;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import dev.razafindratelo.arsmedia.model.token.Token;
import dev.razafindratelo.arsmedia.model.token.TokenPair;
import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.TokenRepository;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.model.token.JToken;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

  private static final String TEST_EMAIL = "test-email@example.com";
  private static final String INACTIVE_EMAIL = "inactive@example.com";
  private static final String NONEXISTENT_EMAIL = "nonexistent@example.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String INACTIVE_PHONE = "+1234567891";
  private static final String TEST_USERNAME = "testuser";
  private static final String INACTIVE_USERNAME = "inactiveuser";
  private static final String TEST_PASSWORD = "encodedPassword123";
  private static final String INACTIVE_PASSWORD = "encodedPassword456";

  private static final String INVALID_TOKEN = "invalid-token";
  private static final String NONEXISTENT_TOKEN = "nonexistent-token";
  private static final String MOCK_ACCESS_TOKEN_PREFIX = "mock-access-token-";
  private static final String MOCK_REFRESH_TOKEN_PREFIX = "mock-refresh-token-";

  private static final String TOKEN_EXPIRED_MSG = "Token expired";
  private static final String TOKEN_TYPE_MISMATCH_MSG = "Token type mismatch";

  private static final int EXPECTED_TOKEN_PAIR_SIZE = 2;
  private static final int EXPECTED_THREE_TOKENS = 3;
  private static final int EXPECTED_TWO_ACCESS_TOKENS = 2;
  private static final int EXPECTED_ONE_TOKEN = 1;
  private static final int EXPECTED_ZERO_TOKENS = 0;

  private static final long ONE_HOUR_AGO = 1L;

  @Autowired private TokenService subject;
  @Autowired private UserRepository userRepository;
  @Autowired private TokenRepository tokenRepository;
  @Autowired private UserService userService;
  @Autowired private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    cleanupTestData();
    createAndActivateTestUser();
  }

  @AfterEach
  void tearDown() {
    cleanupTestData();
  }

  @Test
  void should_generate_token_pair_successfully() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);

    assertValidTokenPair(tokenPair);
    verifyTokensStoredInDatabase();
    verifyAccessTokenStored();
    verifyRefreshTokenStored();
  }

  @Test
  void should_validate_valid_token_successfully() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    var validationResult = subject.validateToken(accessTokenValue, TokenType.ACCESS_TOKEN);

    assertTrue(validationResult.valid());
    assertEquals(TEST_EMAIL, validationResult.userEmail());
  }

  @Test
  void should_return_invalid_for_expired_token() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    expireToken(accessTokenValue);

    var validationResult = subject.validateToken(accessTokenValue, TokenType.ACCESS_TOKEN);

    assertInvalidTokenWithReason(validationResult);
    verifyTokenInvalidated(accessTokenValue);
  }

  @Test
  void should_return_invalid_for_wrong_token_type() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var accessTokenValue = tokenPair.accessToken().value();

    when(jwtUtil.validateToken(accessTokenValue)).thenReturn(true);

    var validationResult = subject.validateToken(accessTokenValue, TokenType.REFRESH_TOKEN);

    assertFalse(validationResult.valid());
    assertTrue(validationResult.reason().contains(TOKEN_TYPE_MISMATCH_MSG));
  }

  @Test
  void should_refresh_token_pair_successfully() {
    var originalTokenPair = subject.generateTokenPair(TEST_EMAIL);
    var refreshTokenValue = originalTokenPair.refreshToken().value();

    mockJwtUtilForRefresh(refreshTokenValue);

    var newTokenPair = subject.refreshTokenPair(refreshTokenValue);

    assertNotNull(newTokenPair);
    assertNotEquals(originalTokenPair.accessToken().value(), newTokenPair.accessToken().value());
    verifyTokenInvalidated(refreshTokenValue);
  }

  @Test
  void should_throw_exception_when_refreshing_invalid_token() {
    when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);

    assertThrows(InvalidTokenException.class, () -> subject.refreshTokenPair(INVALID_TOKEN));
  }

  @Test
  void should_revoke_token_successfully() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var tokenValue = tokenPair.accessToken().value();

    subject.revokeToken(tokenValue, TEST_EMAIL);

    verifyTokenInvalidated(tokenValue);
  }

  @Test
  void should_throw_exception_when_revoking_nonexistent_token() {
    assertThrows(
        TokenNotFoundException.class, () -> subject.revokeToken(NONEXISTENT_TOKEN, TEST_EMAIL));
  }

  @Test
  void should_revoke_all_user_tokens_successfully() {
    subject.generateTokenPair(TEST_EMAIL);
    subject.generateAccessToken(TEST_EMAIL);

    verifyActiveTokenCount(EXPECTED_THREE_TOKENS);

    subject.revokeAllUserTokens(TEST_EMAIL);

    verifyActiveTokenCount(EXPECTED_ZERO_TOKENS);
  }

  @Test
  void should_revoke_tokens_by_type_successfully() {
    subject.generateTokenPair(TEST_EMAIL);
    subject.generateAccessToken(TEST_EMAIL);

    verifyActiveTokenCountByType(TokenType.ACCESS_TOKEN, EXPECTED_TWO_ACCESS_TOKENS);

    subject.revokeUserTokensByType(TEST_EMAIL, TokenType.ACCESS_TOKEN);

    verifyActiveTokenCountByType(TokenType.ACCESS_TOKEN, EXPECTED_ZERO_TOKENS);
    verifyActiveTokenCountByType(TokenType.REFRESH_TOKEN, EXPECTED_ONE_TOKEN);
  }

  @Test
  void should_cleanup_expired_tokens_successfully() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);

    expireToken(tokenPair.accessToken().value());

    var expiredTokensBefore = findExpiredValidTokens();
    assertEquals(EXPECTED_ONE_TOKEN, expiredTokensBefore.size());

    subject.cleanupExpiredTokens();

    var expiredTokensAfter = findExpiredValidTokens();
    assertTrue(expiredTokensAfter.isEmpty());
    verifyTokenInvalidated(tokenPair.accessToken().value());
  }

  @Test
  void should_get_active_user_tokens_successfully() {
    subject.generateTokenPair(TEST_EMAIL);

    var activeTokens = subject.getActiveUserTokens(TEST_EMAIL);

    assertEquals(EXPECTED_TOKEN_PAIR_SIZE, activeTokens.size());
    assertTrue(activeTokens.stream().allMatch(Token::isValid));
    assertTrue(activeTokens.stream().allMatch(token -> token.user().getEmail().equals(TEST_EMAIL)));
  }

  @Test
  void should_get_user_tokens_by_type_successfully() {
    subject.generateTokenPair(TEST_EMAIL);

    var accessTokens = subject.getUserTokensByType(TEST_EMAIL, TokenType.ACCESS_TOKEN);

    assertEquals(EXPECTED_ONE_TOKEN, accessTokens.size());
    assertEquals(TokenType.ACCESS_TOKEN, accessTokens.getFirst().type());
    assertEquals(TEST_EMAIL, accessTokens.getFirst().user().getEmail());
  }

  @Test
  void should_find_token_by_value_successfully() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var tokenValue = tokenPair.accessToken().value();

    var foundToken = subject.findTokenByValue(tokenValue);

    assertNotNull(foundToken);
    assertEquals(tokenValue, foundToken.value());
    assertEquals(TEST_EMAIL, foundToken.user().getEmail());
  }

  @Test
  void should_throw_exception_when_finding_nonexistent_token() {
    assertThrows(
        RessourceNotFoundException.class, () -> subject.findTokenByValue(NONEXISTENT_TOKEN));
  }

  @Test
  void should_check_token_validity_correctly() {
    var tokenPair = subject.generateTokenPair(TEST_EMAIL);
    var tokenValue = tokenPair.accessToken().value();

    var isValid = subject.isTokenValid(tokenValue);

    assertTrue(isValid);
  }

  @Test
  void should_return_false_for_invalid_token() {
    var isValid = subject.isTokenValid(INVALID_TOKEN);

    assertFalse(isValid);
  }

  @Test
  void should_throw_exception_for_inactive_user() {
    createInactiveUser();

    assertThrows(UserNotActivatedException.class, () -> subject.generateTokenPair(INACTIVE_EMAIL));
  }

  @Test
  void should_throw_exception_for_nonexistent_user() {
    assertThrows(EntityNotFoundException.class, () -> subject.generateTokenPair(NONEXISTENT_EMAIL));
  }

  private void cleanupTestData() {
    tokenRepository.deleteAll();
    userRepository.deleteByEmail(TEST_EMAIL);
  }

  private void createAndActivateTestUser() {
    UserCreationRequest request =
        new UserCreationRequest(
            TokenServiceIT.TEST_EMAIL,
            TokenServiceIT.TEST_PHONE,
            TokenServiceIT.TEST_USERNAME,
            UserRole.USER,
            TokenServiceIT.TEST_PASSWORD);
    userService.create(request);
    userService.updateActivationStatusByEmail(TEST_EMAIL, true);
  }

  private void createInactiveUser() {
    UserCreationRequest request =
        new UserCreationRequest(
            INACTIVE_EMAIL, INACTIVE_PHONE, INACTIVE_USERNAME, UserRole.USER, INACTIVE_PASSWORD);
    userService.create(request);

    var jUser = userRepository.findByEmail(INACTIVE_EMAIL).orElseThrow();
    jUser.setActivated(false);
    userRepository.save(jUser);
  }

  private void assertValidTokenPair(TokenPair tokenPair) {
    assertNotNull(tokenPair);
    assertNotNull(tokenPair.accessToken());
    assertNotNull(tokenPair.refreshToken());
    assertEquals(TokenServiceIT.TEST_EMAIL, tokenPair.accessToken().user().getEmail());
    assertEquals(TokenServiceIT.TEST_EMAIL, tokenPair.refreshToken().user().getEmail());
  }

  private void assertInvalidTokenWithReason(TokenValidationResult validationResult) {
    assertFalse(validationResult.valid());
    assertEquals(TokenServiceIT.TOKEN_EXPIRED_MSG, validationResult.reason());
  }

  private void verifyTokensStoredInDatabase() {
    var savedTokens = tokenRepository.findAll();
    assertEquals(TokenServiceIT.EXPECTED_TOKEN_PAIR_SIZE, savedTokens.size());
  }

  private void verifyAccessTokenStored() {
    var accessToken =
        tokenRepository.findAll().stream()
            .filter(token -> token.getType() == TokenType.ACCESS_TOKEN)
            .findFirst();

    assertTrue(accessToken.isPresent());
    assertTrue(accessToken.get().isValid());
    assertNotNull(accessToken.get().getUser());
  }

  private void verifyRefreshTokenStored() {
    var refreshToken =
        tokenRepository.findAll().stream()
            .filter(token -> token.getType() == TokenType.REFRESH_TOKEN)
            .findFirst();

    assertTrue(refreshToken.isPresent());
    assertTrue(refreshToken.get().isValid());
    assertNotNull(refreshToken.get().getUser());
  }

  private void verifyTokenInvalidated(String tokenValue) {
    var invalidatedToken = tokenRepository.findByValue(tokenValue);
    assertTrue(invalidatedToken.isPresent());
    assertFalse(invalidatedToken.get().isValid());
  }

  private void verifyActiveTokenCount(int expectedCount) {
    var activeTokens = tokenRepository.findByUserEmailAndIsValid(TokenServiceIT.TEST_EMAIL, true);
    assertEquals(expectedCount, activeTokens.size());
  }

  private void verifyActiveTokenCountByType(TokenType type, int expectedCount) {
    var tokens =
        tokenRepository.findByUserEmailAndTypeAndIsValid(TokenServiceIT.TEST_EMAIL, type, true);
    assertEquals(expectedCount, tokens.size());
  }

  private void expireToken(String tokenValue) {
    var jToken = tokenRepository.findByValue(tokenValue).orElseThrow();
    jToken.setExpiration(LocalDateTime.now().minusHours(ONE_HOUR_AGO));
    tokenRepository.save(jToken);
  }

  private List<JToken> findExpiredValidTokens() {
    return tokenRepository.findByIsValidAndExpirationBefore(true, LocalDateTime.now());
  }

  private void mockJwtUtilForRefresh(String refreshTokenValue) {
    when(jwtUtil.validateToken(refreshTokenValue)).thenReturn(true);
    when(jwtUtil.extractUsername(refreshTokenValue)).thenReturn(TEST_EMAIL);
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
                return MOCK_ACCESS_TOKEN_PREFIX + user.getEmail() + "-" + UUID.randomUUID();
              });

      when(jwtUtil.createToken(anyMap(), anyString(), any(Duration.class)))
          .thenAnswer(
              invocation -> {
                var subject = invocation.getArgument(1, String.class);
                return MOCK_REFRESH_TOKEN_PREFIX + subject + "-" + UUID.randomUUID();
              });

      when(jwtUtil.validateToken(anyString())).thenReturn(true);
      when(jwtUtil.extractUsername(anyString())).thenReturn(TEST_EMAIL);

      return jwtUtil;
    }
  }
}
