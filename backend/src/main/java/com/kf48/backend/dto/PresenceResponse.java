package com.kf48.backend.dto;

import com.kf48.backend.domain.Presence;

public record PresenceResponse(Long id, Long sessionId, Long etudiantId, String source) {

    public static PresenceResponse depuis(Presence presence) {
        return new PresenceResponse(
                presence.getId(), presence.getSessionId(),
                presence.getEtudiantId(), presence.getSource().name());
    }
}