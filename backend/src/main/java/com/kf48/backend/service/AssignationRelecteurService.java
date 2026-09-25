package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.RelectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * RG4 : un étudiant ne relit jamais son propre exercice.
 * RG5 : un exercice a un seul relecteur.
 * RG6 : relecteur tiré au sort parmi les présents ; si aucun n'est disponible,
 *       l'exercice reste DEPOSE et l'assignation est retentée à chaque nouvelle présence.
 *
 * Appelé après chaque dépôt d'exercice ET après chaque nouvelle présence sur la session,
 * car l'un ou l'autre peut faire apparaître un candidat qui n'existait pas avant.
 */
@Service
public class AssignationRelecteurService {

    private final ExerciceRepository exerciceRepository;
    private final PresenceRepository presenceRepository;
    private final RelectureRepository relectureRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public AssignationRelecteurService(ExerciceRepository exerciceRepository,
                                        PresenceRepository presenceRepository,
                                        RelectureRepository relectureRepository) {
        this.exerciceRepository = exerciceRepository;
        this.presenceRepository = presenceRepository;
        this.relectureRepository = relectureRepository;
    }

    /**
     * Tente d'assigner un relecteur à chaque exercice encore DEPOSE de la session.
     * Idempotent : un exercice déjà assigné n'est plus retourné par la requête
     * (son statut n'est plus DEPOSE), donc un second appel ne crée pas de doublon.
     */
    @Transactional
    public void tenterAssignerPourSession(Long sessionId) {
        List<Exercice> enAttente = exerciceRepository.findBySessionIdAndStatut(sessionId, Exercice.Statut.DEPOSE);
        if (enAttente.isEmpty()) {
            return;
        }

        List<Long> presents = presenceRepository.findEtudiantIdsBySessionId(sessionId);
        if (presents.isEmpty()) {
            return; // RG6 : personne n'est encore arrivé, on retentera
        }

        for (Exercice exercice : enAttente) {
            List<Long> candidats = presents.stream()
                    .filter(id -> !id.equals(exercice.getEtudiantId())) // RG4
                    .toList();

            if (candidats.isEmpty()) {
                continue; // RG6 : reste DEPOSE, retenté à la prochaine présence
            }

            Long relecteurId = candidats.get(aleatoire.nextInt(candidats.size()));
            relectureRepository.save(new Relecture(exercice.getId(), relecteurId)); // RG5
            exercice.marquerEnAttenteRelecture();
            // Les entités findBySessionIdAndStatut sont managées : le statut est persisté à la fin
            // de la transaction englobante (dirty checking), sans appel explicite à save().
        }
    }
}