package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceResponse;
import com.kf48.backend.dto.RemplacerLienRequest;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciceService {

    private final SessionRepository sessionRepository;
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;
    private final AssignationRelecteurService assignationRelecteurService;

    public ExerciceService(SessionRepository sessionRepository,
                           ExerciceRepository exerciceRepository,
                           RelectureRepository relectureRepository,
                           AssignationRelecteurService assignationRelecteurService) {
        this.sessionRepository = sessionRepository;
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
        this.assignationRelecteurService = assignationRelecteurService;
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

        Exercice exercice = exerciceRepository.save(
                new Exercice(requete.sessionId(), requete.etudiantId(), requete.lien()));

        // RG6 : le dépôt peut lui-même rendre l'assignation possible (un autre étudiant est déjà présent)
        assignationRelecteurService.tenterAssignerPourSession(requete.sessionId());

        // La réponse reflète l'état réel après tentative : EN_ATTENTE_RELECTURE si un relecteur
        // a pu être tiré, DEPOSE sinon (personne n'est encore présent).
        return ExerciceResponse.depuis(exercice);
    }

    /**
     * EF4 / RG12 : l'étudiant remplace le lien de son exercice tant qu'aucun relecteur
     * n'est assigné. Dès qu'une relecture existe, le remplacement est refusé (409), même
     * si la relecture n'a pas encore été rendue.
     */
    @Transactional
    public ExerciceResponse remplacerLien(Long exerciceId, RemplacerLienRequest requete) {
        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "EXERCICE_INCONNU"));

        // RG12 : on teste l'EXISTENCE d'une relecture, pas le statut de l'exercice.
        // Le statut pourrait rester DEPOSE dans un cas limite, alors que la seule
        // réalité qui compte est « un relecteur a-t-il été désigné ? ».
        if (relectureRepository.existsByExerciceId(exerciceId)) {
            throw new MetierException(HttpStatus.CONFLICT, "RELECTEUR_DEJA_ASSIGNE");
        }

        exercice.remplacerLien(requete.lien()); // entité managée : persistée par dirty checking
        return ExerciceResponse.depuis(exercice);
    }
}