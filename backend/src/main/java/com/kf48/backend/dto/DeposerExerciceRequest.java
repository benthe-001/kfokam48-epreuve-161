package com.kf48.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DeposerExerciceRequest(
        @NotNull(message = "CHAMP_MANQUANT") Long sessionId,
        @NotNull(message = "CHAMP_MANQUANT") Long etudiantId,
        // @NotBlank est indispensable : @Pattern laisse passer null, ce qui finit
        // en erreur SQL (colonne NOT NULL) au lieu d'un 400 lisible.
        @NotBlank(message = "CHAMP_MANQUANT")
        @Pattern(regexp = "^https?://.+", message = "LIEN_INVALIDE") String lien
) {}