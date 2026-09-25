package com.kf48.relectures.dto;

import com.kfokam48.relectures.domain.SessionCours;
import java.time.OffsetDateTime;

public record SessionResponse(Long id, String code, OffsetDateTime ouvertureAt, OffsetDateTime expirationAt) {

    public static SessionResponse depuis(SessionCours session) {
        return new SessionResponse(session.getId(), session.getCode(), session.getOuvertureAt(), session.getExpirationAt());
    }
}