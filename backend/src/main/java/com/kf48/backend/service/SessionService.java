package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
public class SessionService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sans 0/O/1/I ambigus
    private static final int LONGUEUR_CODE = 6;
    private static final long DUREE_VALIDITE_MINUTES = 15; // RG1

    private final SessionRepository sessionRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public SessionResponse ouvrir(OuvrirSessionRequest requete) {
        OffsetDateTime ouverture = OffsetDateTime.now();
        String code = genererCodeUnique(); // RG17
        SessionCours session = new SessionCours(
                requete.titre(), requete.promotionId(), code,
                ouverture, ouverture.plusMinutes(DUREE_VALIDITE_MINUTES));
        return SessionResponse.depuis(sessionRepository.save(session));
    }

    private String genererCodeUnique() {
        String code;
        do {
            code = genererCode();
        } while (sessionRepository.existsByCode(code)); // RG17 : unicité garantie
        return code;
    }

    private String genererCode() {
        StringBuilder sb = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            sb.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}