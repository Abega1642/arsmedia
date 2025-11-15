package dev.razafindratelo.arsmedia.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import dev.razafindratelo.arsmedia.model.User;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ApiKeyGeneratorTest {

  public static final String TEST_EMAIL = "test@example.com";
  public static final String ERROR_LOG = "Failed to generate API key";
  private ApiKeyGenerator subject;
  private User testUser;
  private LocalDateTime testCreationTime;

  @BeforeEach
  void setUp() {
    subject = new ApiKeyGenerator();
    ReflectionTestUtils.setField(subject, "apiKeySignature", "test-secret-signature-123");

    testUser = new User();
    testUser.setEmail(TEST_EMAIL);
    testUser.setId("user-123");

    testCreationTime = LocalDateTime.of(2025, 10, 16, 10, 30, 30);
  }

  @Test
  void should_generate_apiKey_with_correct_format() {
    String apiKey = subject.apply(testUser, testCreationTime);

    assertThat(apiKey).isNotNull();
    assertThat(apiKey).isNotBlank();
    assertThat(isValidBase64Url(apiKey)).isTrue();
  }

  @Test
  void should_generate_different_keys_for_different_users() {
    var anotherUser = new User();
    anotherUser.setEmail("another@example.com");

    var apiKey1 = subject.apply(testUser, testCreationTime);
    var apiKey2 = subject.apply(anotherUser, testCreationTime);

    assertThat(apiKey1).isNotEqualTo(apiKey2);
  }

  @Test
  void should_generate_different_keys_for_same_user_at_different_times() {
    var anotherTime = testCreationTime.plusHours(1);

    var apiKey1 = subject.apply(testUser, testCreationTime);
    var apiKey2 = subject.apply(testUser, anotherTime);

    assertThat(apiKey1).isNotEqualTo(apiKey2);
  }

  @Test
  void shouldGenerateSameKeyForSameInputs() {
    var apiKey1 = subject.apply(testUser, testCreationTime);
    var apiKey2 = subject.apply(testUser, testCreationTime);

    assertThat(apiKey1).isEqualTo(apiKey2);
  }

  @Test
  void should_generate_key_with_correct_structure() {
    var apiKey = subject.apply(testUser, testCreationTime);

    var decoded = new String(Base64.getUrlDecoder().decode(apiKey));
    String[] parts = decoded.split("\\|");

    assertThat(parts).hasSize(3);
    assertThat(parts[0]).isEqualTo(TEST_EMAIL);
    assertThat(parts[1]).isEqualTo("2025-10-16T10:30:30");
    assertThat(parts[2]).hasSize(16);
  }

  @Test
  void should_handle_special_characters_in_email() {
    var specialUser = new User();
    specialUser.setEmail("test+special@example.com");

    var apiKey = subject.apply(specialUser, testCreationTime);

    assertThat(apiKey).isNotNull();
    assertThat(apiKey).isNotBlank();
    assertThat(isValidBase64Url(apiKey)).isTrue();
  }

  @Test
  void should_throw_exception_when_user_is_null() {
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> subject.apply(null, testCreationTime));

    assertThat(exception.getMessage()).contains(ERROR_LOG);
  }

  @Test
  void should_throw_exception_when_creation_time_is_null() {
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> subject.apply(testUser, null));

    assertThat(exception.getMessage()).contains(ERROR_LOG);
  }

  @Test
  void should_generate_verifiable_signature() throws Exception {
    var apiKey = subject.apply(testUser, testCreationTime);

    var decoded = new String(Base64.getUrlDecoder().decode(apiKey));
    String[] parts = decoded.split("\\|");

    var email = parts[0];
    var creationTime = parts[1];
    var receivedSignature = parts[2];

    var data = email + "|" + creationTime;
    var expectedSignature = calculateExpectedSignature(data);

    assertThat(receivedSignature).isEqualTo(expectedSignature);
  }

  @Test
  void should_generate_different_keys_with_different_signatures() {
    var anotherGenerator = new ApiKeyGenerator();
    ReflectionTestUtils.setField(anotherGenerator, "apiKeySignature", "different-secret");

    var apiKey1 = subject.apply(testUser, testCreationTime);
    var apiKey2 = anotherGenerator.apply(testUser, testCreationTime);

    assertThat(apiKey1).isNotEqualTo(apiKey2);
  }

  private boolean isValidBase64Url(String str) {
    try {
      Base64.getUrlDecoder().decode(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private String calculateExpectedSignature(String data) throws Exception {
    var hmacSHA256 = "HmacSHA256";
    Mac hmac = Mac.getInstance(hmacSHA256);
    SecretKeySpec secretKey =
        new SecretKeySpec(
            "test-secret-signature-123".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            hmacSHA256);
    hmac.init(secretKey);
    byte[] hmacBytes = hmac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return bytesToHex(hmacBytes).substring(0, 16);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
