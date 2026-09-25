package com.kf48.backend.service;

import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PresenceService {

    private final SessionRepository sessionRepository;
    private final PresenceRepository presenceRepository;

    public PresenceService(SessionRepository sessionRepository, PresenceRepository presenceRepository) {
        this.sessionRepository = sessionRepository;
        this.presenceRepository = presenceRepository;
    }

    @Transactional
    public PresenceResponse marquer(MarquerPresenceRequest requete) {
        SessionCours session = sessionRepository.findByCode(requete.code())
                .orElseThrow(() -> new MetierException(HttpStatus.BAD_REQUEST, "CODE_INCONNU"));

        if (OffsetDateTime.now().isAfter(session.getExpirationAt())) { // RG1
            throw new MetierException(HttpStatus.GONE, "CODE_EXPIRE");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) { // RG18
            throw new MetierException(HttpStatus.CONFLICT, "DEJA_PRESENT");
        }

        Presence presence = new Presence(session.getId(), requete.etudiantId(), Presence.Source.ETUDIANT); // RG13
        return PresenceResponse.depuis(presenceRepository.save(presence));
    }
}