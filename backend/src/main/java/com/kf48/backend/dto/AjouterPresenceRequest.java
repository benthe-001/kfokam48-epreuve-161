package com.kf48.backend.dto;

import jakarta.validation.constraints.NotNull;

/** EF10 : le formateur ajoute une présence manuelle. */
public record AjouterPresenceRequest(
        @NotNull(message = "CHAMP_MANQUANT") Long etudiantId
) {}