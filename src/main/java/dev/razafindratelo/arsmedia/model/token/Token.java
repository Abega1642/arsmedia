package dev.razafindratelo.arsmedia.model.token;

import dev.razafindratelo.arsmedia.model.User;
import java.time.LocalDateTime;

public record Token(
    String id,
    User user,
    LocalDateTime creation,
    LocalDateTime expiration,
    TokenType type,
    boolean isValid,
    String value) {}
