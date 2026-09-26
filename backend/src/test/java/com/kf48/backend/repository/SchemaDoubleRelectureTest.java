package com.kf48.backend.repository;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Issue #27 : verifie le schema APRES la migration V4__double_relecture.
 *
 * La migration remplace uk_relecture_exercice UNIQUE (exercice_id) par
 * uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id). Ces tests
 * ne verifient donc pas une intention de code : ils observent la base, et
 * verrouillent le fait que V4 a bien ete appliquee et qu'elle produit
 * exactement l'invariant voulu.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaDoubleRelectureTest {

    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private RelectureRepository relectureRepository;
    @Autowired private EtudiantRepository etudiantRepository;

    private Long exerciceId;

    @BeforeEach
    void creerExercice() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        Long sessionId = sessionRepository.save(new SessionCours(
                "Seance schema", 1L, "SCHM1", ouverture, ouverture.plusMinutes(15))).getId();
        exerciceId = exerciceRepository.save(
                new Exercice(sessionId, 1L, "https://exemple.test/exo")).getId();
    }

    @Test
    void deuxRelecteursDistinctsSontDesormaisAutorises() { // RG5 révisée
        // Échouer ici signifierait que V4 n'a pas été appliquée et que l'ancienne
        // contrainte unique sur exercice_id est toujours en place.
        relectureRepository.saveAndFlush(new Relecture(exerciceId, 2L));
        relectureRepository.saveAndFlush(new Relecture(exerciceId, 3L));

        assertThat(relectureRepository.findAll().stream()
                .filter(r -> r.getExerciceId().equals(exerciceId)))
                .hasSize(2);
    }

    @Test
    void leMemeRelecteurNePeutPasRelireDeuxFoisLeMemeExercice() {
        relectureRepository.saveAndFlush(new Relecture(exerciceId, 2L));

        // La contrainte porte sur le COUPLE : c'est ce qui empêche la répétition
        // d'un relecteur tout en autorisant deux relecteurs différents.
        assertThatThrownBy(() -> relectureRepository.saveAndFlush(new Relecture(exerciceId, 2L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lesDonneesCreesAvantLaMigrationSurvivent() {
        // V2 insère 2 promotions et 4 étudiants AVANT l'exécution de V4. Vérifier
        // qu'ils sont toujours là prouve qu'aucune donnée existante n'a été perdue
        // par la migration, qui ne contient ni DELETE ni UPDATE.
        assertThat(etudiantRepository.count()).isEqualTo(4);
        assertThat(etudiantRepository.findByPromotionIdOrderById(1L))
                .extracting(com.kf48.backend.domain.Etudiant::getId)
                .containsExactly(1L, 2L);
        assertThat(etudiantRepository.findByPromotionIdOrderById(2L))
                .extracting(com.kf48.backend.domain.Etudiant::getId)
                .containsExactly(3L, 4L);

        // Ces étudiants, créés avant la migration, restent utilisables comme relecteurs.
        relectureRepository.saveAndFlush(new Relecture(exerciceId, 3L));
        assertThat(relectureRepository.findByExerciceId(exerciceId)).isPresent();
    }
}
