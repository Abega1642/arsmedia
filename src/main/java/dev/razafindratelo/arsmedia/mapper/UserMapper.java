package dev.razafindratelo.arsmedia.mapper;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.health.model.RUser;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import java.util.UUID;

public class UserMapper {
  public static User toUser(JUser jUser) {
    return new User(
        jUser.getId(),
        jUser.getEmail(),
        jUser.getPseudo(),
        jUser.getPhoneNumber(),
        jUser.getImageProfileBucketKey(),
        jUser.getRole(),
        jUser.getPassword(),
        jUser.isActivated(),
        jUser.getCreatedAt(),
        jUser.getUpdatedAt());
  }

  public static JUser toJUser(User user) {
    return new JUser(
        user.getId(),
        user.getEmail(),
        user.getPseudo(),
        user.getPhoneNumber(),
        user.getImageProfileBucketKey(),
        user.getPassword(),
        user.getRole(),
        user.isActivated(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  public static User toUser(RUser rUser) {
    return new User(
        UUID.randomUUID().toString(),
        rUser.email(),
        rUser.pseudo(),
        rUser.phoneNumber(),
        "no-image-profile",
        rUser.role(),
        rUser.password(),
        false,
        now(),
        now());
  }
}
