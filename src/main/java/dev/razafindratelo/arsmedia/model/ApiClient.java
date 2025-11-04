package dev.razafindratelo.arsmedia.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApiClient(
    @NotNull @NotBlank String id,
    @NotNull @NotBlank @Email String clientEmail,
    @NotNull @NotBlank String phoneNumber,
    @NotNull @NotBlank String clientName) {}
