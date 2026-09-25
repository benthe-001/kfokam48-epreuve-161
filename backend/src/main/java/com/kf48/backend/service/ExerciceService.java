package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciceService {

    private final SessionRepository sessionRepository;
    private final ExerciceRepository exerciceRepository;

    public ExerciceService(SessionRepository sessionRepository, ExerciceRepository exerciceRepository) {
        this.sessionRepository = sessionRepository;
        this.exerciceRepository = exerciceRepository;
    }

    @Transactional
    public ExerciceResponse deposer(DeposerExerciceRequest requete) {
        SessionCours session = sessionRepository.findById(requete.sessionId())
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "SESSION_INCONNUE"));

        // RG11 : le dépôt reste possible après expiration du code, tant que la session n'est pas clôturée.
        // L'expiration de session_cours.expiration_at n'est volontairement pas testée ici (RG1 ne
        // concerne que le code de présence) : seule une clôture explicite ferme le dépôt.
        if (session.getStatut() == SessionCours.Statut.CLOTUREE) {
            throw new MetierException(HttpStatus.CONFLICT, "SESSION_CLOTUREE");
        }

        if (exerciceRepository.existsBySessionIdAndEtudiantId(requete.sessionId(), requete.etudiantId())) { // RG19
            throw new MetierException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE");
        }

        Exercice exercice = new Exercice(requete.sessionId(), requete.etudiantId(), requete.lien());
        return ExerciceResponse.depuis(exerciceRepository.save(exercice));
    }
}