package dev.razafindratelo.arsmedia.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CompressedVideo(
    @NotBlank @NotNull String id, @NotNull Video parent, @NotNull LocalDateTime createdAt) {}
