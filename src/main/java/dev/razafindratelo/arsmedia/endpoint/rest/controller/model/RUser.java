package dev.razafindratelo.arsmedia.endpoint.rest.controller.model;

import dev.razafindratelo.arsmedia.model.classifier.UserRole;

public record RUser(
    String email, String phoneNumber, String pseudo, UserRole role, String password) {}
