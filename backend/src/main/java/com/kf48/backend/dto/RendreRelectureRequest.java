package com.kf48.backend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RendreRelectureRequest(

        // RG8 : note entiere de 0 a 20. Le type est BigDecimal et non Integer car un Integer
        // rejetterait 12.5 en erreur de deserialisation JSON (code REQUETE_INCONNUE) alors que
        // le contrat exige NOTE_INVALIDE pour toute note non entiere. @Digits(integer=2, fraction=0)
        // rejette les decimales avec le bon code, @Min/@Max les bornes.
        // integer=2 et non 1 : la note 20 comporte deux chiffres et serait rejetee a tort.
        @NotNull(message = "NOTE_INVALIDE")
        @Min(value = 0, message = "NOTE_INVALIDE")
        @Max(value = 20, message = "NOTE_INVALIDE")
        @Digits(integer = 2, fraction = 0, message = "NOTE_INVALIDE")
        BigDecimal note,

        @NotBlank(message = "CHAMP_MANQUANT")
        String commentaire
) {}