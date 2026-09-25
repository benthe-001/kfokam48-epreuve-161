package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.LigneTableauResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EF11 / RG10 / RG15 : le tableau récapitulatif.
 * La promotion 1 de V2__demo_data.sql contient les étudiants 1 et 2 ;
 * la promotion 2 contient les étudiants 3 et 4 et sert de témoin d'isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TableauServiceTest {

    private static final String LIEN = "https://github.com/etudiant/exercice-1";
    private static final Long PROMO_A = 1L;
    private static final Long PROMO_B = 2L;

    @Autowired private TableauService tableauService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private PresenceRepository presenceRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private Long sessionA;
    private Long sessionB;

    @BeforeEach
    void creerSessions() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        sessionA = sessionRepository.save(new SessionCours(
                "Seance A", PROMO_A, "AAA111", ouverture, ouverture.plusMinutes(15))).getId();
        sessionB = sessionRepository.save(new SessionCours(
                "Seance B", PROMO_A, "BBB222", ouverture, ouverture.plusMinutes(15))).getId();
    }

    private void presencer(Long sessionId, Long etudiantId) {
        presenceRepository.save(new Presence(sessionId, etudiantId, Presence.Source.ETUDIANT));
    }

    /** Depose un exercice et renvoie la relecture qui lui est assignée. */
    private Relecture deposerAvecRelecture(Long sessionId, Long etudiantId, Long relecteurId) {
        Long exerciceId = exerciceRepository.save(
                new Exercice(sessionId, etudiantId, LIEN)).getId();
        return relectureRepository.save(new Relecture(exerciceId, relecteurId));
    }

    private LigneTableauResponse ligne(long etudiantId) {
        return tableauService.consulter(PROMO_A).stream()
                .filter(l -> l.etudiantId().equals(etudiantId))
                .findFirst().orElseThrow();
    }

    @Test
    void uneLigneParEtudiantDeLaPromotion() {
        List<LigneTableauResponse> tableau = tableauService.consulter(PROMO_A);

        assertThat(tableau).hasSize(2); // étudiants 1 et 2
        assertThat(tableau).extracting(LigneTableauResponse::etudiantId)
                .containsExactly(1L, 2L);
    }

    @Test
    void promotionInconnueRenvoie404() {
        assertThatThrownBy(() -> tableauService.consulter(999999L))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("PROMOTION_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    @Test
    void comptageDesPresencesSurToutesLesSessions() {
        presencer(sessionA, 1L);
        presencer(sessionB, 1L);
        presencer(sessionA, 2L);

        assertThat(ligne(1L).presences()).isEqualTo(2L);
        assertThat(ligne(2L).presences()).isEqualTo(1L);
    }

    @Test
    void comptageDesExercicesDeposes() {
        exerciceRepository.save(new Exercice(sessionA, 1L, LIEN));
        exerciceRepository.save(new Exercice(sessionB, 1L, LIEN));

        assertThat(ligne(1L).exercicesDeposes()).isEqualTo(2L);
        assertThat(ligne(2L).exercicesDeposes()).isZero();
    }

    @Test
    void laMoyenneIgnoreLesExercicesNonNotes() { // RG15 : exclusion explicite
        Relecture notee = deposerAvecRelecture(sessionA, 1L, 2L);
        notee.rendre(10, "OK");
        deposerAvecRelecture(sessionB, 1L, 2L); // assignée mais pas rendue : ne compte pas

        // 10 et non 5 : la relecture non rendue est exclue de la moyenne
        assertThat(ligne(1L).moyenne()).isEqualTo(10.0);
    }

    @Test
    void laMoyenneMoyenneLesExercicesNotes() { // RG15 : plusieurs sessions confondues
        Relecture premiere = deposerAvecRelecture(sessionA, 1L, 2L);
        premiere.rendre(10, "OK");
        Relecture seconde = deposerAvecRelecture(sessionB, 1L, 2L);
        seconde.rendre(16, "Très bien");

        assertThat(ligne(1L).moyenne()).isEqualTo(13.0);
    }

    @Test
    void laMoyenneEstNulleSansExerciceNote() { // RG15 : null et non 0
        deposerAvecRelecture(sessionA, 1L, 2L); // assignée, pas rendue

        LigneTableauResponse etudiant = ligne(1L);
        assertThat(etudiant.moyenne()).isNull();
        assertThat(etudiant.exercicesDeposes()).isEqualTo(1L); // l'exercice existe pourtant
    }

    @Test
    void lesRelecturesEnAttenteSontComptees() { // RG10 : visible comme tel
        deposerAvecRelecture(sessionA, 1L, 2L); // assignée à l'étudiant 2
        deposerAvecRelecture(sessionB, 1L, 2L); // deuxième, encore en attente
        Relecture rendue = deposerAvecRelecture(sessionA, 3L, 2L);
        rendue.rendre(12, "OK"); // celle-ci est terminée, elle ne compte plus

        assertThat(ligne(2L).relecturesEnAttente()).isEqualTo(2L);
    }

    @Test
    void lesDonneesDUneAutrePromotionSontExclues() {
        Long sessionAutre = sessionRepository.save(new SessionCours(
                "Seance promo 2", PROMO_B, "CCC333", OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(15))).getId();
        presencer(sessionAutre, 3L);
        exerciceRepository.save(new Exercice(sessionAutre, 3L, LIEN));

        assertThat(ligne(1L).presences()).isZero();
        assertThat(ligne(1L).exercicesDeposes()).isZero();
        assertThat(tableauService.consulter(PROMO_B)).hasSize(2); // étudiants 3 et 4
    }

    @Test
    void unePromotionSansSessionDonneUnTableauAZero() {
        // Ce n'est pas une erreur : les étudiants de la promotion restent listés
        LigneTableauResponse etudiant = ligne(1L);
        assertThat(etudiant.presences()).isZero();
        assertThat(etudiant.moyenne()).isNull();
    }
}
