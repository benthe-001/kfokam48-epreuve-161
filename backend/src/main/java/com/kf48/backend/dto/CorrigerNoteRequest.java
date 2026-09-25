package com.kf48.backend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * EF7 : correction d'une note deja rendue. Meme validation que RendreRelectureRequest
 * (RG8), declarada a part pour que chaque endpoint garde son propre schema dans Swagger
 * et puisse evoluer independamment.
 */
public record CorrigerNoteRequest(

        @NotNull(message = "NOTE_INVALIDE")
        @Min(value = 0, message = "NOTE_INVALIDE")
        @Max(value = 20, message = "NOTE_INVALIDE")
        @Digits(integer = 2, fraction = 0, message = "NOTE_INVALIDE")
        BigDecimal note,

        @NotBlank(message = "CHAMP_MANQUANT")
        String commentaire
) {}