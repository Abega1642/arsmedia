package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TokenPairRequest(
    @NotNull @NotBlank String userId, @Email @NotNull @NotBlank String userEmail) {}
