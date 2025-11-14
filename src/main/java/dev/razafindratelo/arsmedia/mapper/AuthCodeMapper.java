package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.model.AuthCode;
import dev.razafindratelo.arsmedia.repository.model.JAuthCode;

public class AuthCodeMapper {

  private AuthCodeMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static AuthCode toModel(JAuthCode jAuthCode) {
    return new AuthCode(
        jAuthCode.getId(),
        UserMapper.toUser(jAuthCode.getUser()),
        jAuthCode.getCode(),
        jAuthCode.getCreatedAt(),
        jAuthCode.getDeadline());
  }

  public static JAuthCode toJAuthCode(AuthCode authCode) {
    return new JAuthCode(
        authCode.id(),
        UserMapper.toJUser(authCode.owner()),
        authCode.code(),
        authCode.createdAt(),
        authCode.deadline());
  }
}
