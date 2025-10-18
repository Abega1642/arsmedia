package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import java.time.LocalDateTime;

public record RTokenPair(RToken accessToken, RToken refreshToken, LocalDateTime requestTime) {}
