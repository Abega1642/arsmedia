package dev.razafindratelo.arsmedia.model;

import java.time.LocalDateTime;

public record ApiKey(
    String id, User owner, String apiKey, LocalDateTime creation, LocalDateTime expiration) {}
