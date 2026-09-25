package com.kf48.backend.dto;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;

/**
 * EF8 : détail de l'exercice vu par l'étudiant relu.
 *
 * RG7 : l'identité du relecteur n'apparaît ici sous aucune forme. La note et le
 * commentaire sont recopiés explicitement champ par champ — et non via une sérialisation
 * automatique de l'entité — pour qu'aucune évolution du modèle ne puisse les exposer
 * par mégarde. Le nom du relecteur n'est même pas connu de l'API : seule la table
 * `relecture` porte cette information, et elle n'est jamais interrogée pour ce DTO.
 *
 * note et commentaire valent null tant que la relecture n'est pas rendue.
 */
public record ExerciceDetailResponse(Long id, Long sessionId, Long etudiantId,
                                    String lien, String statut,
                                    Integer note, String commentaire) {

    public static ExerciceDetailResponse depuis(Exercice exercice, Relecture relecture) {
        return new ExerciceDetailResponse(
                exercice.getId(),
                exercice.getSessionId(),
                exercice.getEtudiantId(),
                exercice.getLien(),
                exercice.getStatut().name(),
                relecture == null ? null : relecture.getNote(),
                relecture == null ? null : relecture.getCommentaire());
    }
}
