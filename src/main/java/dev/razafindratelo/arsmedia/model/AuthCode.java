package dev.razafindratelo.arsmedia.model;

import static java.util.UUID.randomUUID;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public record AuthCode(
    String id, User owner, String code, LocalDateTime createdAt, LocalDateTime deadline) {

  public static String generateCode() {
    int number = ThreadLocalRandom.current().nextInt(0, 100_000);
    return String.format("%05d", number);
  }

  public static AuthCode generate(User user) {
    var id = randomUUID().toString();
    var code = generateCode();
    var createdAt = LocalDateTime.now();
    var deadline = createdAt.plusMinutes(10);
    return new AuthCode(id, user, code, createdAt, deadline);
  }
}
