package dev.razafindratelo.arsmedia.repository.mapper;

import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.repository.model.JUser;

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
}
