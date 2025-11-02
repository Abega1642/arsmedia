package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record AuthCodeResponse(@Email @NotBlank String sentTo, LocalDateTime sentAt) {}
