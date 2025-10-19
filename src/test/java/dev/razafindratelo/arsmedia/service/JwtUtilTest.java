package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.exception.JwtProcessingException;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {
  private final String TEST_EMAIL = "test@example.com";
  private JwtUtil subject;

  @BeforeEach
  void setUp() {
    String testSecret = "test-secret-key-that-is-long-enough-for-hs256-123456";
    subject = new JwtUtil(testSecret);
  }

  @Test
  void should_initialize_with_valid_secret() {
    assertNotNull(subject);
    assertNotNull(ReflectionTestUtils.getField(subject, "secretKey"));
    assertNotNull(ReflectionTestUtils.getField(subject, "jwtParser"));
  }

  @Test
  void should_throw_exception_when_secret_is_null() {
    assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
  }

  @Test
  void should_throw_exception_when_secret_is_empty() {
    assertThrows(IllegalStateException.class, () -> new JwtUtil(""));
  }

  @Test
  void should_generate_valid_token_for_user() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(subject.validateToken(token));
  }

  @Test
  void should_generate_token_with_base_claims() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertEquals(user.getEmail(), subject.extractUsername(token));
    assertEquals(user.getId(), subject.extractUserId(token));
    assertEquals(user.getPseudo(), subject.extractAllClaims(token).get("pseudo"));

    var roles = subject.extractRoles(token);
    assertNotNull(roles);
    assertTrue(roles.contains("ROLE_USER"));
  }

  @Test
  void should_generate_token_with_additional_claims() {
    var user = createTestUser();
    var additionalClaims = new HashMap<String, Object>();
    additionalClaims.put("custom_claim", "custom_value");
    additionalClaims.put("token_type", "ACCESS");

    var token = subject.generateToken(user, additionalClaims);

    assertEquals("custom_value", subject.extractAllClaims(token).get("custom_claim"));
    assertEquals("ACCESS", subject.extractTokenType(token));
  }

  @Test
  void should_create_token_with_custom_subject_and_duration() {
    var claims = new HashMap<String, Object>();
    claims.put("user_id", "custom-user-123");
    var subject = "custom@example.com";
    var duration = Duration.ofHours(1);

    var token = this.subject.createToken(claims, subject, duration);

    assertNotNull(token);
    assertEquals(subject, this.subject.extractUsername(token));
    assertEquals("custom-user-123", this.subject.extractUserId(token));

    var expiration = this.subject.extractExpiration(token);
    assertTrue(expiration.isAfter(LocalDateTime.now()));
  }

  @Test
  void should_validate_token_correctly() {
    var user = createTestUser();
    var token = subject.generateToken(user);

    assertTrue(subject.validateToken(token));
  }

  @Test
  void should_return_false_for_invalid_token() {
    var invalidToken = "invalid.token.here";

    assertFalse(subject.validateToken(invalidToken));
  }

  @Test
  void should_return_false_for_null_token() {
    assertFalse(subject.validateToken(null));
  }

  @Test
  void should_return_false_for_empty_token() {
    assertFalse(subject.validateToken(""));
    assertFalse(subject.validateToken("   "));
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
    var roles = subject.extractRoles(token);

    assertNotNull(roles);
    assertTrue(roles.contains("ROLE_ADMIN"));
  }

  @Test
  void should_extract_token_type_from_token() {
    var user = createTestUser();
    var claims = new HashMap<String, Object>();
    claims.put("token_type", "REFRESH");
    var token = subject.generateToken(user, claims);
    var tokenType = subject.extractTokenType(token);

    assertEquals("REFRESH", tokenType);
  }

  @Test
  void should_extract_expiration_from_token() {
    var user = createTestUser();
    var token = subject.generateToken(user);
    var expiration = subject.extractExpiration(token);

    assertNotNull(expiration);
    assertTrue(expiration.isAfter(LocalDateTime.now()));
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

    assertNotNull(claims);
    assertEquals(user.getEmail(), claims.getSubject());
    assertEquals(user.getId(), claims.get("user_id"));
    assertNotNull(claims.get("roles"));
  }

  @Test
  void should_throw_exception_when_extracting_claims_from_invalid_token() {
    var invalidToken = "invalid.token.here";

    assertThrows(JwtProcessingException.class, () -> subject.extractAllClaims(invalidToken));
  }

  @Test
  void should_throw_exception_when_creating_token_with_null_subject() {
    var claims = new HashMap<String, Object>();
    var duration = Duration.ofHours(1);

    assertThrows(IllegalArgumentException.class, () -> subject.createToken(claims, null, duration));
  }

  @Test
  void should_throw_exception_when_creating_token_with_empty_subject() {
    var claims = new HashMap<String, Object>();
    var duration = Duration.ofHours(1);

    assertThrows(IllegalArgumentException.class, () -> subject.createToken(claims, "", duration));
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
    var user = new User();
    user.setId("user-123");
    user.setEmail(TEST_EMAIL);
    user.setPseudo("testuser");
    user.setRole(UserRole.USER);

    var token = subject.generateToken(user);

    assertNotNull(token);
    assertTrue(subject.validateToken(token));
  }

  @Test
  void should_generate_different_tokens_for_same_user() throws InterruptedException {
    var user = createTestUser();
    var token1 = subject.generateToken(user);
    Thread.sleep(1000);
    var token2 = subject.generateToken(user);

    assertNotEquals(token1, token2);
    assertTrue(subject.validateToken(token1));
    assertTrue(subject.validateToken(token2));
  }

  private User createTestUser() {
    var user = new User();
    user.setId("user-123");
    user.setEmail(TEST_EMAIL);
    user.setPseudo("testuser");
    user.setPassword("password");
    user.setRole(UserRole.USER);
    user.setActivated(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    return user;
  }

  private User createAdminUser() {
    var user = createTestUser();
    user.setRole(UserRole.ADMIN);
    return user;
  }
}
