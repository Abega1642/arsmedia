package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.model.User;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.function.BiFunction;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator implements BiFunction<User, LocalDateTime, String> {

  private static final String SEPARATOR = "|";
  private static final String ALGORITHM = "HmacSHA256";

  @Value("${api.key.signature}")
  private String apiKeySignature;

  @Override
  public String apply(User user, LocalDateTime creationTime) {
    try {
      String data = user.getEmail() + SEPARATOR + creationTime.toString();
      String signature = calculateHmac(data, apiKeySignature);

      return encodeBase64(data + SEPARATOR + signature);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate API key", e);
    }
  }

  private String calculateHmac(String data, String key) throws Exception {
    Mac hmac = Mac.getInstance(ALGORITHM);
    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    hmac.init(secretKey);
    byte[] hmacBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hmacBytes).substring(0, 16);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  private String encodeBase64(String data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes());
  }
}
