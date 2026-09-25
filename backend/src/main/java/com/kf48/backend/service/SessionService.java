package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionClotureResponse;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.http.HttpStatus;
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

    /**
     * EF9 / RG14 : le formateur clôture explicitement une session.
     *
     * La clôture est un acte volontaire, distinct de l'expiration automatique du code
     * au bout de 15 minutes (RG1) : le code peut être expiré sans que la session soit
     * close. Une session déjà clôturée est refusée (409 SESSION_DEJA_CLOTUREE) plutôt
     * que d'être clôturée une seconde fois — une clôture est un fait, pas un compteur.
     *
     * RG10 : la clôture ne touche à aucun exercice. Ceux qui n'ont pas encore de
     * relecture rendue restent « en attente » et restent consultables ; le tableau
     * récapitulatif les listing tels quels (EF11).
     */
    @Transactional
    public SessionClotureResponse cloturer(Long sessionId) {
        SessionCours session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "SESSION_INCONNUE"));

        if (session.getStatut() == SessionCours.Statut.CLOTUREE) {
            throw new MetierException(HttpStatus.CONFLICT, "SESSION_DEJA_CLOTUREE");
        }

        session.cloturer(); // entité managée : persistée par dirty checking
        return SessionClotureResponse.depuis(session);
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