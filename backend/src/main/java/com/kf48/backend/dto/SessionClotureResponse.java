package com.kf48.backend.dto;

import com.kf48.backend.domain.SessionCours;

import java.time.OffsetDateTime;

/**
 * EF9 : retour de la clôture. Le contrat ne fixe pas de corps de réponse pour cette
 * route ; on expose le minimum utile — l'identifiant, le statut et l'instant de clôture —
 * pour que le formateur puisse confirmer l'action sans refaire un GET.
 */
public record SessionClotureResponse(Long id, String statut, OffsetDateTime clotureAt) {

    public static SessionClotureResponse depuis(SessionCours session) {
        return new SessionClotureResponse(session.getId(), session.getStatut().name(), session.getClotureAt());
    }
}
