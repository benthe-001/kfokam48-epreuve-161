package com.kf48.backend.dto;

import com.kf48.backend.domain.Exercice;

public record ExerciceResponse(Long id, String statut) {

    public static ExerciceResponse depuis(Exercice exercice) {
        return new ExerciceResponse(exercice.getId(), exercice.getStatut().name());
    }
}