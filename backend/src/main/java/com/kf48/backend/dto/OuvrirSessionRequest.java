package com.kf48.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OuvrirSessionRequest(
        @NotBlank(message = "CHAMP_MANQUANT") String titre,
        @NotNull(message = "CHAMP_MANQUANT") Long promotionId
) {}