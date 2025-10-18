package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.model.token.TokenType.ACCESS_TOKEN;
import static dev.razafindratelo.arsmedia.model.token.TokenType.REFRESH_TOKEN;
import static java.lang.String.format;
import static java.time.Duration.ofDays;
import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.mapper.TokenMapper;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.token.Token;
import dev.razafindratelo.arsmedia.model.token.TokenPair;
import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.TokenRepository;
import dev.razafindratelo.arsmedia.repository.model.token.JToken;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.NotSupportedException;
import jakarta.validation.constraints.Email;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class TokenService {
  private final Duration ACCESS_TOKEN_DURATION = ofDays(2);
  private final Duration REFRESH_TOKEN_DURATION = ofDays(4);
  private UserService userService;
  private TokenRepository repository;

  public Token findByValue(String value) {
    if (value == null || value.isEmpty())
      throw new IllegalArgumentException("Token value cannot be null or empty");

    var jToken =
        repository
            .findByValue(value)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No token found with given value %s", value)));
    return TokenMapper.toModel(jToken);
  }

  public TokenPair requestForToken(@Email String userEmail, String refreshTokenValue)
      throws NotSupportedException {
    throw new NotSupportedException("TODO"); // TODO: to be implemented later
  }

  public Token disableToken(String refreshTokenValue) throws NotSupportedException {
    throw new NotSupportedException("TODO"); // TODO: to be implemented later;
  }

  public TokenPair generateTokenPair(@Email String userEmail) {
    var accessToken = generateAccessToken(userEmail);
    var refreshToken = generateRefreshToken(userEmail);
    return new TokenPair(accessToken, refreshToken, now());
  }

  public Token generateAccessToken(@Email String userEmail) {
    return generate(userEmail, ACCESS_TOKEN_DURATION, ACCESS_TOKEN);
  }

  public Token generateRefreshToken(@Email String userEmail) {
    return generate(userEmail, REFRESH_TOKEN_DURATION, REFRESH_TOKEN);
  }

  public Token generate(@Email String userEmail, Duration duration, TokenType type) {
    if (userEmail == null || userEmail.isEmpty())
      throw new IllegalArgumentException(
          "User email could not be null or empty for token generation.");

    if (duration == null) throw new IllegalArgumentException("Token duration could not be null.");

    if (type == null)
      throw new IllegalArgumentException(
          format(
              "Token type should be specified to allow the system generate it. Please choose"
                  + " between: %s and %s",
              ACCESS_TOKEN, REFRESH_TOKEN));

    var owner = userService.findByEmail(userEmail);
    LocalDateTime creation = now();
    var expiration = creation.plus(duration);
    var value = "TO-BE-IMPLEMENTED"; // TODO: wait for JWTUtil;
    var jToken =
        new JToken(
            UUID.randomUUID().toString(),
            UserMapper.toJUser(owner),
            true,
            value,
            type,
            creation,
            expiration);
    return TokenMapper.toModel(repository.save(jToken));
  }
}
