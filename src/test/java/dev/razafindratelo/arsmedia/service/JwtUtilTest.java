package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.exception.JwtProcessingException;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private static final String TEST_EMAIL = "test@example.com";
  private static final String CUSTOM_EMAIL = "custom@example.com";
  private static final String TEST_USER_ID = "user-123";
  private static final String CUSTOM_USER_ID = "custom-user-123";
  private static final String TEST_PSEUDO = "testuser";
  private static final String TEST_PASSWORD = "password";

  private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hs256-123456";
  private static final String INVALID_TOKEN = "invalid.token.here";
  private static final String EMPTY_STRING = "";
  private static final String WHITESPACE_STRING = "   ";

  private static final String CLAIM_CUSTOM = "custom_claim";
  private static final String CLAIM_TOKEN_TYPE = "token_type";
  private static final String CLAIM_USER_ID = "user_id";
  private static final String CLAIM_PSEUDO = "pseudo";
  private static final String CLAIM_ROLES = "roles";

  private static final String CUSTOM_VALUE = "custom_value";
  private static final String TOKEN_TYPE_ACCESS = "ACCESS";
  private static final String TOKEN_TYPE_REFRESH = "REFRESH";

  private static final String ROLE_USER = "ROLE_USER";
  private static final String ROLE_ADMIN = "ROLE_ADMIN";

  private static final String FIELD_SECRET_KEY = "secretKey";
  private static final String FIELD_JWT_PARSER = "jwtParser";

  private static final long TOKEN_GENERATION_DELAY_MS = 1000L;
  private static final int TOKEN_DURATION_HOURS = 1;

  private JwtUtil subject;

  @BeforeEach
  void setUp() {
    subject = new JwtUtil(TEST_SECRET);
  }

  @Test
  void should_initialize_with_valid_secret() {
    assertNotNull(subject);
    assertNotNull(ReflectionTestUtils.getField(subject, FIELD_SECRET_KEY));
    assertNotNull(ReflectionTestUtils.getField(subject, FIELD_JWT_PARSER));
  }

  @Test
  void should_throw_exception_when_secret_is_null() {
    assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
  }

  @Test
  void should_throw_exception_when_secret_is_empty() {
    assertThrows(IllegalStateException.class, () -> new JwtUtil(EMPTY_STRING));
  }

  @Test
  void should_generate_valid_token_for_user() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertTokenGeneratedSuccessfully(token);
  }

  @Test
  void should_generate_token_with_base_claims() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertBaseClaimsPresent(token, user);
    assertRolesContain(token, ROLE_USER);
  }

  @Test
  void should_generate_token_with_additional_claims() {
    var user = createTestUser();
    var additionalClaims = createAdditionalClaims();

    var token = subject.generateToken(user, additionalClaims);

    assertEquals(CUSTOM_VALUE, subject.extractAllClaims(token).get(CLAIM_CUSTOM));
    assertEquals(TOKEN_TYPE_ACCESS, subject.extractTokenType(token));
  }

  @Test
  void should_create_token_with_custom_subject_and_duration() {
    var claims = createCustomClaims();
    var duration = Duration.ofHours(TOKEN_DURATION_HOURS);

    var token = subject.createToken(claims, CUSTOM_EMAIL, duration);

    assertCustomTokenCreated(token);
  }

  @Test
  void should_validate_token_correctly() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertTrue(subject.validateToken(token));
  }

  @Test
  void should_return_false_for_invalid_token() {
    assertFalse(subject.validateToken(INVALID_TOKEN));
  }

  @Test
  void should_return_false_for_null_token() {
    assertFalse(subject.validateToken(null));
  }

  @Test
  void should_return_false_for_empty_token() {
    assertFalse(subject.validateToken(EMPTY_STRING));
    assertFalse(subject.validateToken(WHITESPACE_STRING));
  }

  @Test
  void should_extract_username_from_token() {
    var user = createTestUser();
    var token = subject.generateToken(user);
    var username = subject.extractUsername(token);

    assertEquals(user.getEmail(), username);
  }

  @Test
  void should_extract_user_id_from_token() {
    var user = createTestUser();
    var token = subject.generateToken(user);
    var userId = subject.extractUserId(token);

    assertEquals(user.getId(), userId);
  }

  @Test
  void should_extract_roles_from_token() {
    var user = createAdminUser();
    var token = subject.generateToken(user);

    assertRolesContain(token, ROLE_ADMIN);
  }

  @Test
  void should_extract_token_type_from_token() {
    var user = createTestUser();
    var claims = createTokenTypeClaims();
    var token = subject.generateToken(user, claims);
    var tokenType = subject.extractTokenType(token);

    assertEquals(TOKEN_TYPE_REFRESH, tokenType);
  }

  @Test
  void should_extract_expiration_from_token() {
    var user = createTestUser();
    var token = subject.generateToken(user);
    var expiration = subject.extractExpiration(token);

    assertExpirationValid(expiration);
  }

  @Test
  void should_check_if_token_is_expired() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertFalse(subject.isTokenExpired(token));
  }

  @Test
  void should_extract_all_claims_from_token() {
    var user = createTestUser();
    var token = subject.generateToken(user);
    var claims = subject.extractAllClaims(token);

    assertAllClaimsPresent(claims, user);
  }

  @Test
  void should_throw_exception_when_extracting_claims_from_invalid_token() {
    assertThrows(JwtProcessingException.class, () -> subject.extractAllClaims(INVALID_TOKEN));
  }

  @Test
  void should_throw_exception_when_creating_token_with_null_subject() {
    var claims = new HashMap<String, Object>();
    var duration = Duration.ofHours(TOKEN_DURATION_HOURS);

    assertThrows(IllegalArgumentException.class, () -> subject.createToken(claims, null, duration));
  }

  @Test
  void should_throw_exception_when_creating_token_with_empty_subject() {
    var claims = new HashMap<String, Object>();
    var duration = Duration.ofHours(TOKEN_DURATION_HOURS);

    assertThrows(
        IllegalArgumentException.class, () -> subject.createToken(claims, EMPTY_STRING, duration));
  }

  @Test
  void should_throw_exception_when_creating_token_with_null_duration() {
    var claims = new HashMap<String, Object>();

    assertThrows(
        IllegalArgumentException.class, () -> subject.createToken(claims, TEST_EMAIL, null));
  }

  @Test
  void should_throw_exception_when_creating_token_with_zero_duration() {
    var claims = new HashMap<String, Object>();

    assertThrows(
        IllegalArgumentException.class,
        () -> subject.createToken(claims, TEST_EMAIL, Duration.ZERO));
  }

  @Test
  void should_handle_user_with_null_authorities_gracefully() {
    var user = createUserWithoutAuthorities();

    var token = subject.generateToken(user);

    assertTokenGeneratedSuccessfully(token);
  }

  @Test
  void should_generate_different_tokens_for_same_user() throws InterruptedException {
    var user = createTestUser();
    var token1 = subject.generateToken(user);
    Thread.sleep(TOKEN_GENERATION_DELAY_MS);
    var token2 = subject.generateToken(user);

    assertNotEquals(token1, token2);
    assertTrue(subject.validateToken(token1));
    assertTrue(subject.validateToken(token2));
  }

  private User createTestUser() {
    return createUser(UserRole.USER);
  }

  private User createAdminUser() {
    return createUser(UserRole.ADMIN);
  }

  private User createUserWithoutAuthorities() {
    var user = new User();
    user.setId(TEST_USER_ID);
    user.setEmail(TEST_EMAIL);
    user.setPseudo(TEST_PSEUDO);
    user.setRole(UserRole.USER);
    return user;
  }

  private User createUser(UserRole role) {
    var user = new User();
    user.setId(JwtUtilTest.TEST_USER_ID);
    user.setEmail(JwtUtilTest.TEST_EMAIL);
    user.setPseudo(JwtUtilTest.TEST_PSEUDO);
    user.setPassword(TEST_PASSWORD);
    user.setRole(role);
    user.setActivated(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    return user;
  }

  private HashMap<String, Object> createAdditionalClaims() {
    var claims = new HashMap<String, Object>();
    claims.put(CLAIM_CUSTOM, CUSTOM_VALUE);
    claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
    return claims;
  }

  private HashMap<String, Object> createCustomClaims() {
    var claims = new HashMap<String, Object>();
    claims.put(CLAIM_USER_ID, CUSTOM_USER_ID);
    return claims;
  }

  private HashMap<String, Object> createTokenTypeClaims() {
    var claims = new HashMap<String, Object>();
    claims.put(CLAIM_TOKEN_TYPE, JwtUtilTest.TOKEN_TYPE_REFRESH);
    return claims;
  }

  private void assertTokenGeneratedSuccessfully(String token) {
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(subject.validateToken(token));
  }

  private void assertBaseClaimsPresent(String token, User user) {
    assertEquals(user.getEmail(), subject.extractUsername(token));
    assertEquals(user.getId(), subject.extractUserId(token));
    assertEquals(user.getPseudo(), subject.extractAllClaims(token).get(CLAIM_PSEUDO));
  }

  private void assertRolesContain(String token, String expectedRole) {
    var roles = subject.extractRoles(token);
    assertNotNull(roles);
    assertTrue(roles.contains(expectedRole));
  }

  private void assertCustomTokenCreated(String token) {
    assertNotNull(token);
    assertEquals(JwtUtilTest.CUSTOM_EMAIL, subject.extractUsername(token));
    assertEquals(JwtUtilTest.CUSTOM_USER_ID, subject.extractUserId(token));

    var expiration = subject.extractExpiration(token);
    assertExpirationValid(expiration);
  }

  private void assertExpirationValid(LocalDateTime expiration) {
    assertNotNull(expiration);
    assertTrue(expiration.isAfter(LocalDateTime.now()));
  }

  private void assertAllClaimsPresent(Claims claims, User user) {
    assertNotNull(claims);
    assertEquals(user.getEmail(), claims.getSubject());
    assertEquals(user.getId(), claims.get(CLAIM_USER_ID));
    assertNotNull(claims.get(CLAIM_ROLES));
  }
}
