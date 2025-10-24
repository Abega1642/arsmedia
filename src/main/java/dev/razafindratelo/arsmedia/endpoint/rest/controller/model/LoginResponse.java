package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import java.time.LocalDateTime;

public record LoginResponse(String message, String email, LocalDateTime requestTime, RUser user) {}
