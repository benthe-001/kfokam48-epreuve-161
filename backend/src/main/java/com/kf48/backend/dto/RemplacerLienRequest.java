package com.kf48.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RemplacerLienRequest(
        // Meme validation que le depot : @Pattern laisse passer null, ce qui finirait
        // en erreur SQL sur la colonne NOT NULL au lieu d'un 400 lisible.
        @NotBlank(message = "CHAMP_MANQUANT")
        @Pattern(regexp = "^https?://.+", message = "LIEN_INVALIDE") String lien
) {}