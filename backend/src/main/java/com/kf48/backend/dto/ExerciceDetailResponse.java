package com.kf48.backend.dto;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kf48.backend.service.AssignationRelecteurService.RELECTEURS_REQUIS;

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
                                    Double note, boolean noteProvisoire,
                                    String commentaire) {

    /**
     * RG17 : la note retenue est la moyenne des relectures rendues.
     * RG18 : si un seul relecteur a rendu, sa note est affichée mais marquée
     * provisoire ; l'exercice reste en attente de la seconde.
     * Sans aucune relecture rendue, la note est nulle et n'est pas provisoire
     * (il n'y a rien à afficher).
     *
     * commentaire : les deux pairs écrivent chacun le leur, et l'identité de chacun
     * est interdite (RG7). On ne peut donc pas distinguer « le commentaire de A »
     * de « celui de B » : les commentaires rendus sont concaténés, séparés par une
     * ligne vide, et présentés comme un retour global.
     */
    public static ExerciceDetailResponse depuis(Exercice exercice, List<Relecture> relectures) {
        List<Relecture> rendues = relectures.stream()
                .filter(r -> r.getRendueAt() != null && r.getNote() != null)
                .toList();

        // average() renvoie un OptionalDouble, dont orElse(null) n'existe pas :
        // on ne demande la valeur que lorsqu'il y a effectivement des notes.
        Double moyenne = rendues.isEmpty() ? null
                : rendues.stream().mapToInt(Relecture::getNote).average().getAsDouble();

        String commentaires = rendues.isEmpty() ? null
                : rendues.stream()
                    .map(Relecture::getCommentaire)
                    .filter(Objects::nonNull)
                    .filter(c -> !c.isBlank())
                    .collect(Collectors.joining("\n\n"));

        boolean provisoire = !rendues.isEmpty() && rendues.size() < RELECTEURS_REQUIS;

        return new ExerciceDetailResponse(
                exercice.getId(),
                exercice.getSessionId(),
                exercice.getEtudiantId(),
                exercice.getLien(),
                exercice.getStatut().name(),
                moyenne,
                provisoire,
                commentaires);
    }
}
