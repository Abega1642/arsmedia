package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RUser(
    @Email @NotBlank String email,
    String phoneNumber,
    String pseudo,
    UserRole role,
    String password) {}
