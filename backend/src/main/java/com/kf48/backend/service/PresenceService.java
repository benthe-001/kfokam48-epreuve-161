package com.kf48.backend.service;

import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.domain.TentativeBlocage;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.SessionRepository;
import com.kf48.backend.repository.TentativeBlocageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PresenceService {

    private final SessionRepository sessionRepository;
    private final PresenceRepository presenceRepository;
    private final TentativeBlocageRepository tentativeBlocageRepository;

    public PresenceService(SessionRepository sessionRepository,
                           PresenceRepository presenceRepository,
                           TentativeBlocageRepository tentativeBlocageRepository) {
        this.sessionRepository = sessionRepository;
        this.presenceRepository = presenceRepository;
        this.tentativeBlocageRepository = tentativeBlocageRepository;
    }

    @Transactional
    public PresenceResponse marquer(MarquerPresenceRequest requete) {
        TentativeBlocage tentative = tentativeBlocageRepository.findById(requete.etudiantId())
                .orElseGet(() -> new TentativeBlocage(requete.etudiantId()));

        if (tentative.estBloque()) { // RG3
            throw new MetierException(HttpStatus.TOO_MANY_REQUESTS, "ETUDIANT_BLOQUE");
        }

        SessionCours session = sessionRepository.findByCode(requete.code()).orElse(null);
        if (session == null) {
            tentative.enregistrerEchec(); // RG3 : seul CODE_INCONNU compte comme brute-force
            tentativeBlocageRepository.save(tentative);
            throw new MetierException(HttpStatus.BAD_REQUEST, "CODE_INCONNU");
        }

        if (OffsetDateTime.now().isAfter(session.getExpirationAt())) { // RG1
            throw new MetierException(HttpStatus.GONE, "CODE_EXPIRE");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) { // RG18
            throw new MetierException(HttpStatus.CONFLICT, "DEJA_PRESENT");
        }

        tentative.reinitialiser(); // RG3 : succes -> compteur remis a zero
        tentativeBlocageRepository.save(tentative);

        Presence presence = new Presence(session.getId(), requete.etudiantId(), Presence.Source.ETUDIANT); // RG13
        return PresenceResponse.depuis(presenceRepository.save(presence));
    }
}