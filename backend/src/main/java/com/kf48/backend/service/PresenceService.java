package com.kf48.backend.service;

import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.domain.TentativeBlocage;
import com.kf48.backend.dto.AjouterPresenceRequest;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.EtudiantRepository;
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
    private final AssignationRelecteurService assignationRelecteurService;
    private final EtudiantRepository etudiantRepository;

    public PresenceService(SessionRepository sessionRepository,
                           PresenceRepository presenceRepository,
                           TentativeBlocageRepository tentativeBlocageRepository,
                           AssignationRelecteurService assignationRelecteurService,
                           EtudiantRepository etudiantRepository) {
        this.sessionRepository = sessionRepository;
        this.presenceRepository = presenceRepository;
        this.tentativeBlocageRepository = tentativeBlocageRepository;
        this.assignationRelecteurService = assignationRelecteurService;
        this.etudiantRepository = etudiantRepository;
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

        Presence presence = presenceRepository.save(
                new Presence(session.getId(), requete.etudiantId(), Presence.Source.ETUDIANT)); // RG13

        // RG6 : un nouvel arrivant peut débloquer les exercices encore sans relecteur
        assignationRelecteurService.tenterAssignerPourSession(session.getId());

        return PresenceResponse.depuis(presence);
    }


    /**
     * EF10 / RG13 : le formateur ajoute une présence manuelle.
     *
     * RG13 : c'est la seule opération autorisée après l'expiration du code — un
     * formateur doit pouvoir rattraper un étudiant qu'il a oublié au moment de la
     * séance, et l'expiration du code est automatique (15 min) alors que la clôture
     * est un acte volontaire : refuser ici pour cause d'expiration rendrait la
     * fonctionnalité inutile. En revanche la clôture est respectée (409).
     *
     * RG6 : une présence enregistrée est aussi un déclencheur d'une nouvelle tentative
     * d'assignation, exactement comme un marquage étudiant.
     */
    @Transactional
    public PresenceResponse ajouterManuellement(Long sessionId, AjouterPresenceRequest requete) {
        SessionCours session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "SESSION_INCONNUE"));

        if (!etudiantRepository.existsById(requete.etudiantId())) {
            throw new MetierException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU");
        }

        if (session.getStatut() == SessionCours.Statut.CLOTUREE) { // RG13
            throw new MetierException(HttpStatus.CONFLICT, "SESSION_CLOTUREE");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(sessionId, requete.etudiantId())) { // RG18
            throw new MetierException(HttpStatus.CONFLICT, "DEJA_PRESENT");
        }

        Presence presence = presenceRepository.save(
                new Presence(sessionId, requete.etudiantId(), Presence.Source.FORMATEUR));

        assignationRelecteurService.tenterAssignerPourSession(sessionId); // RG6

        return PresenceResponse.depuis(presence);
    }
}