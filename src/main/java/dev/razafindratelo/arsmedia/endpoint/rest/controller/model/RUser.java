package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RUser(
    @NotNull @NotBlank String id,
    @NotNull @Email @NotBlank String email,
    @NotNull @NotBlank String phoneNumber,
    @NotNull @NotBlank String pseudo,
    @NotNull @NotBlank String imageProfileBucketKey,
    @NotNull UserRole role) {}
