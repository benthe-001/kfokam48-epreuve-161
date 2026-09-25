package com.kf48.backend.dto;

/**
 * EF11 : une ligne du tableau récapitulatif.
 *
 * RG15 : la moyenne ne porte que sur les exercices notés ; elle vaut null si l'étudiant
 * n'a aucun exercice noté, et non 0 — un 0 se lirait comme une moyenne nulle, ce qui
 * est une autre information.
 *
 * relecturesEnAttente compte les relectures que l'étudiant doit encore rendre, pas
 * celles portant sur ses propres exercices : c'est la charge de travail restante.
 */
public record LigneTableauResponse(Long etudiantId, String nom, long presences,
                                   long exercicesDeposes, Double moyenne,
                                   long relecturesEnAttente) {
}