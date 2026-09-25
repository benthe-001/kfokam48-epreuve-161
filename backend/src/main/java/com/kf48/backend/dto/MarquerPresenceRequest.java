package com.kf48.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarquerPresenceRequest(
        @NotBlank(message = "CHAMP_MANQUANT") String code,
        @NotNull(message = "CHAMP_MANQUANT") Long etudiantId
) {}