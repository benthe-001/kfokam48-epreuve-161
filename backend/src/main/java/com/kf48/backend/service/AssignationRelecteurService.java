package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RG4 : un étudiant ne relit jamais son propre exercice.
 * RG5 (révisée, issue #27) : un exercice est relu par DEUX relecteurs distincts.
 * RG6 : relecteurs tirés au sort parmi les présents ; s'il n'y en a pas assez,
 *       l'exercice reste en attente et l'assignation est retentée à chaque nouvelle
 *       présence enregistrée sur la session.
 *
 * Appelé après chaque dépôt d'exercice ET après chaque nouvelle présence sur la session,
 * car l'un ou l'autre peut faire apparaître un candidat qui n'existait pas avant.
 */
@Service
public class AssignationRelecteurService {

    /** RG5 révisée (issue #27) : nombre de relecteurs requis par exercice. */
    public static final int RELECTEURS_REQUIS = 2;

    private final ExerciceRepository exerciceRepository;
    private final PresenceRepository presenceRepository;
    private final RelectureRepository relectureRepository;
    private final SessionRepository sessionRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public AssignationRelecteurService(ExerciceRepository exerciceRepository,
                                        PresenceRepository presenceRepository,
                                        RelectureRepository relectureRepository,
                                        SessionRepository sessionRepository) {
        this.exerciceRepository = exerciceRepository;
        this.presenceRepository = presenceRepository;
        this.relectureRepository = relectureRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Complete l'assignation de chaque exercice de la session qui n'a pas encore
     * ses deux relecteurs (RG5 revisee).
     *
     * Issue #25 : la session est d'abord verrouillee en ecriture. Sans ce verrou,
     * deux transactions concurrentes lisent le meme exercice et inserent chacune
     * deux relectures ; la seconde viole la contrainte unique du couple
     * (exercice, relecteur) et voit sa transaction entiere annulee, donc une
     * presence perdue. Avec le verrou, la seconde attend, relit apres le commit de
     * la premiere, constate que l'exercice a deja ses deux relecteurs et ne fait
     * rien. Le verrou est sur la session, pas sur l'exercice : c'est le perimetre
     * minimal qui serialise toutes les assignations de cette session, quel que soit
     * l'appelant (depot d'exercice ou marquage de presence).
     */
    @Transactional
    public void tenterAssignerPourSession(Long sessionId) {
        sessionRepository.findByIdPourMiseAJour(sessionId)
                .orElse(null); // verrou pessimiste : bloque jusqu'au commit de la transaction concurrente

        // RG5 révisée : tout exercice non noté, car un exercice déjà partiellement
        // assigné (un seul relecteur trouvé) doit pouvoir recevoir son second.
        List<Exercice> enAttente = exerciceRepository.findBySessionIdAndStatutNot(
                sessionId, Exercice.Statut.NOTE);
        if (enAttente.isEmpty()) {
            return;
        }

        List<Long> presents = presenceRepository.findEtudiantIdsBySessionId(sessionId);
        if (presents.isEmpty()) {
            return; // RG6 : personne n'est encore arrivé, on retentera
        }

        // Un seul chargement pour tous les exercices du lot : sans cela, on
        // interrogerait la base une fois par exercice.
        Map<Long, List<Long>> dejaAssignes = new HashMap<>();
        for (Relecture relecture : relectureRepository.findByExerciceIdIn(
                enAttente.stream().map(Exercice::getId).toList())) {
            dejaAssignes.computeIfAbsent(relecture.getExerciceId(), k -> new ArrayList<>())
                    .add(relecture.getRelecteurId());
        }

        for (Exercice exercice : enAttente) {
            List<Long> dejaChoisis = dejaAssignes.getOrDefault(exercice.getId(), List.of());
            int manquants = RELECTEURS_REQUIS - dejaChoisis.size();
            if (manquants <= 0) {
                continue; // déjà les deux relecteurs
            }

            // RG4 : l'auteur n'est jamais candidat. Les relecteurs déjà choisis non
            // plus : c'est ce qui garantit qu'on tire deux pairs DIFFÉRENTS, et non
            // deux fois le même.
            List<Long> candidats = presents.stream()
                    .filter(id -> !id.equals(exercice.getEtudiantId()))
                    .filter(id -> !dejaChoisis.contains(id))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (candidats.isEmpty()) {
                continue; // RG6 : pas assez de monde, on retentera à la prochaine présence
            }

            Collections.shuffle(candidats, aleatoire);
            int aTirer = Math.min(manquants, candidats.size());
            for (int i = 0; i < aTirer; i++) {
                relectureRepository.save(new Relecture(exercice.getId(), candidats.get(i)));
            }

            if (dejaChoisis.isEmpty()) {
                exercice.marquerEnAttenteRelecture();
            }
            // Si l'exercice avait déjà un relecteur, son statut est déjà
            // EN_ATTENTE_RELECTURE : seul le nombre de relectures change.
            // Les entités findBySessionIdAndStatutNot sont managées : le statut est
            // persisté en fin de transaction (dirty checking).
        }
    }
}
