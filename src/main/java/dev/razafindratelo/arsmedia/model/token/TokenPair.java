package dev.razafindratelo.arsmedia.model.token;

import java.time.LocalDateTime;

public record TokenPair(Token accessToken, Token refreshToken, LocalDateTime requestTime) {}
