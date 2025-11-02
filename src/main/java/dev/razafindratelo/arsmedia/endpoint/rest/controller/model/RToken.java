package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import java.time.LocalDateTime;

public record RToken(
    String id,
    String userId,
    LocalDateTime creation,
    LocalDateTime expiration,
    boolean isValid,
    String value) {}
