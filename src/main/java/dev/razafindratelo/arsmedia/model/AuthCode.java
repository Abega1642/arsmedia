package dev.razafindratelo.arsmedia.model;

import static java.util.UUID.randomUUID;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public record AuthCode(
    String id, User owner, String code, LocalDateTime createdAt, LocalDateTime deadline) {

  private static final int RANDOM_BOUND = 100_000;
  private static final int RANDOM_ORIGIN = 0;

  public static String generateCode() {
    int number = ThreadLocalRandom.current().nextInt(RANDOM_ORIGIN, RANDOM_BOUND);
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
