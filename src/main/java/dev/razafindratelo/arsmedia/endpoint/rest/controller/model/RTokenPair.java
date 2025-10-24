package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RTokenPair(
    @NotNull RToken accessToken,
    @NotNull RToken refreshToken,
    @NotNull LocalDateTime requestTime) {}
