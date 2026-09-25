package com.kf48.backend.dto;

import com.kf48.backend.domain.Relecture;

import java.time.OffsetDateTime;

/**
 * RG7 : l'identite du relecteur n'est volontairement pas exposee dans cette reponse.
 * Meme si l'appelant est le relecteur lui-meme, la regle « jamais l'identite du relecteur »
 * s'applique uniformement a toutes les representations d'une relecture.
 */
public record RelectureResponse(Long id, Long exerciceId, Integer note,
                                String commentaire, OffsetDateTime rendueAt) {

    public static RelectureResponse depuis(Relecture relecture) {
        return new RelectureResponse(relecture.getId(), relecture.getExerciceId(),
                relecture.getNote(), relecture.getCommentaire(), relecture.getRendueAt());
    }
}