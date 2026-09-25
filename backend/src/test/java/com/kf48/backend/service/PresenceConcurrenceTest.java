package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #25 : une presence peut etre perdue quand deux etudiants marquent leur
 * presence en meme temps sur une session contenant un exercice encore DEPOSE.
 *
 * Ce test est volontairement NON transactionnel (@Transactional absent) : il a besoin
 * de vraies transactions concurrentes, ce qu'un test @Transactional ne peut pas
 * reproduire (le rollback automatique masquerait exactement le defaut qu'on cherche).
 *
 * Il nettoie ses propres donnees : la base H2 de test est partagee entre les classes
 * (DB_CLOSE_DELAY=-1) et les autres tests s'appuient sur le rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
class PresenceConcurrenceTest {

    private static final int TOURS = 5; // repetitions : la course est une question de timing
    private static final String PREFIXE_CODE = "CONC";

    @Autowired private PresenceService presenceService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private PresenceRepository presenceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private final List<Long> sessionsCreees = new ArrayList<>();

    @BeforeEach
    void nettoyer() {
        purger();
    }

    @AfterEach
    void nettoyerApres() {
        purger();
    }

    /** Supprime les donnees de ce test, dans l'ordre des dependances de cles etrangeres. */
    private void purger() {
        if (sessionsCreees.isEmpty()) {
            return;
        }
        List<Exercice> exercices = exerciceRepository.findAll().stream()
                .filter(e -> sessionsCreees.contains(e.getSessionId()))
                .toList();
        relectureRepository.deleteAll(relectureRepository.findAll().stream()
                .filter(r -> exercices.stream().anyMatch(e -> e.getId().equals(r.getExerciceId())))
                .toList());
        presenceRepository.deleteAll(presenceRepository.findAll().stream()
                .filter(p -> sessionsCreees.contains(p.getSessionId()))
                .toList());
        exerciceRepository.deleteAll(exercices);
        sessionRepository.deleteAll(sessionRepository.findAll().stream()
                .filter(s -> sessionsCreees.contains(s.getId()))
                .toList());
        sessionsCreees.clear();
    }

    /** Cree une session avec un exercice DEPOSE de l'etudiant 1, et renvoie son code. */
    private String preparerSession(int tour) {
        OffsetDateTime ouverture = OffsetDateTime.now();
        SessionCours session = sessionRepository.save(new SessionCours(
                "Concurrence " + tour, 1L, PREFIXE_CODE + tour,
                ouverture, ouverture.plusMinutes(15)));
        sessionsCreees.add(session.getId());
        // L'auteur est l'etudiant 1 : les etudiants 3 et 4 sont des candidats
        // au tirage du relecteur, c'est ce qui declenche l'assignation en concurrency.
        exerciceRepository.save(new Exercice(session.getId(), 1L, "https://exemple.test/exo"));
        return session.getCode();
    }

    @Test
    void deuxMarquagesSimultanesDoiventTousLesDeuxEtreEnregistres() throws Exception {
        List<String> echecs = new ArrayList<>();

        for (int tour = 0; tour < TOURS; tour++) {
            String code = preparerSession(tour);
            Long sessionId = sessionsCreees.get(sessionsCreees.size() - 1);
            CyclicBarrier depart = new CyclicBarrier(2);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                List<Future<?>> taches = new ArrayList<>();
                for (long etudiantId : new long[] {3L, 4L}) {
                    taches.add(pool.submit(() -> {
                        try {
                            // La barriere force les deux transactions a partir ensemble :
                            // c'est ce qui rend la course reproductible.
                            depart.await(5, TimeUnit.SECONDS);
                            presenceService.marquer(new MarquerPresenceRequest(code, etudiantId));
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    "etudiant " + etudiantId + " : " + e.getMessage(), e);
                        }
                    }));
                }
                for (Future<?> tache : taches) {
                    try {
                        tache.get(20, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        echecs.add(e.getMessage() == null ? e.toString() : e.getMessage());
                    }
                }
            } finally {
                pool.shutdownNow();
            }

            long presences = presenceRepository.findAll().stream()
                    .filter(p -> p.getSessionId().equals(sessionId))
                    .count();
            if (presences != 2) {
                echecs.add("tour " + tour + " : " + presences + " presence(s) au lieu de 2");
            }
        }

        assertThat(echecs).as("Issue #25 : aucune presence ne doit etre perdue").isEmpty();
    }
}
