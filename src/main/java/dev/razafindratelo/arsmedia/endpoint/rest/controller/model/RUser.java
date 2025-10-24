package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RUser(
    @Email @NotBlank String email,
    @NotNull @NotBlank String phoneNumber,
    String pseudo,
    @NotNull UserRole role,
    @NotNull @NotBlank String password) {}
