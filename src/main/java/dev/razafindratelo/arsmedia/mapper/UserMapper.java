package dev.razafindratelo.arsmedia.mapper;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
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

  public static JUser toJUser(UserCreationRequest userCreationRequest) {
    return toJUser(toUser(userCreationRequest));
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

  public static RUser toRUser(User user) {
    return new RUser(
        user.getId(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getPseudo(),
        user.getImageProfileBucketKey(),
        user.getRole());
  }

  public static User toUser(UserCreationRequest userCreationRequest) {
    return new User(
        UUID.randomUUID().toString(),
        userCreationRequest.email(),
        userCreationRequest.pseudo(),
        userCreationRequest.phoneNumber(),
        "no-image-profile",
        userCreationRequest.role(),
        userCreationRequest.password(),
        false,
        now(),
        now());
  }
}
