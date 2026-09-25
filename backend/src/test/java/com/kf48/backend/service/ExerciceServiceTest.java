package com.kf48.backend.service;

import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceDetailResponse;
import com.kf48.backend.dto.ExerciceResponse;
import com.kf48.backend.dto.RemplacerLienRequest;
import com.kf48.backend.dto.RendreRelectureRequest;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciceServiceTest {

    @Autowired private ExerciceService exerciceService;
    @Autowired private RelectureService relectureService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private RelectureRepository relectureRepository;
    @Autowired private PresenceRepository presenceRepository;

    private static final String LIEN = "https://github.com/etudiant/exercice-1";

    private SessionCours session;

    @BeforeEach
    void ouvrirSession() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        session = sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)));
    }

    @Test
    void depotCreeUnExerciceStatutDepose() {
        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        assertThat(reponse.id()).isNotNull();
        assertThat(reponse.statut()).isEqualTo("DEPOSE");
        assertThat(exerciceRepository.existsBySessionIdAndEtudiantId(session.getId(), 1L)).isTrue();
    }

    @Test
    void depotRestePossibleApresExpirationDuCode() { // RG11 : c'est tout l'objet de ce ticket
        OffsetDateTime ouverture = OffsetDateTime.now().minusHours(3);
        SessionCours sessionTerminee = sessionRepository.save(new SessionCours(
                "Seance terminee", 1L, "EXPIRE", ouverture, ouverture.plusMinutes(15)));
        assertThat(sessionTerminee.getExpirationAt()).isBefore(OffsetDateTime.now()); // le code a bien expiré

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(sessionTerminee.getId(), 1L, LIEN));

        assertThat(reponse.statut()).isEqualTo("DEPOSE");
    }

    @Test
    void sessionInconnueRenvoie404SESSION_INCONNUE() {
        assertThatThrownBy(() -> exerciceService.deposer(
                new DeposerExerciceRequest(999999L, 1L, LIEN)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    @Test
    void secondDepotRenvoie409EXERCICE_DEJA_DEPOSE() { // RG19
        exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        assertThatThrownBy(() -> exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void deuxEtudiantsDeposentChacunUnExercice() { // RG19 : la contrainte porte sur le couple
        ExerciceResponse a = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        ExerciceResponse b = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 2L, LIEN));

        assertThat(a.id()).isNotEqualTo(b.id());
    }

    // ---------- EF5 / RG12 : remplacement du lien ----------

    private static final String NOUVEAU_LIEN = "https://github.com/etudiant/exercice-1-v2";

    @Test
    void remplacementAccepteSansRelecteurAssigne() {
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();

        ExerciceResponse reponse = exerciceService.remplacerLien(id, new RemplacerLienRequest(NOUVEAU_LIEN));

        assertThat(exerciceRepository.findById(id).orElseThrow().getLien()).isEqualTo(NOUVEAU_LIEN);
        assertThat(reponse.statut()).isEqualTo("DEPOSE"); // le remplacement ne change pas le statut
    }

    @Test
    void remplacementMetAJourModifieAt() {
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
        assertThat(exerciceRepository.findById(id).orElseThrow().getModifieAt()).isNull();

        exerciceService.remplacerLien(id, new RemplacerLienRequest(NOUVEAU_LIEN));

        assertThat(exerciceRepository.findById(id).orElseThrow().getModifieAt()).isNotNull();
    }

    @Test
    void remplacementRefuseDesQuUnRelecteurEstAssigne() { // RG12 : meme sans relecture rendue
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
        relectureRepository.save(new Relecture(id, 2L)); // relecteur designe, rendu_at encore null

        assertThatThrownBy(() -> exerciceService.remplacerLien(id, new RemplacerLienRequest(NOUVEAU_LIEN)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RELECTEUR_DEJA_ASSIGNE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });

        assertThat(exerciceRepository.findById(id).orElseThrow().getLien()).isEqualTo(LIEN); // lien inchangé
    }

    @Test
    void exerciceInconnuRenvoie404EXERCICE_INCONNU() {
        assertThatThrownBy(() -> exerciceService.remplacerLien(999999L, new RemplacerLienRequest(NOUVEAU_LIEN)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("EXERCICE_INCONNU"));
    }

    // ---- EF8 / RG7 : consultation de sa note et de son commentaire ----

    /** Depose un exercice et renvoie son id ; l'etudiant 2 est present pour forcer l'assignation. */
    private Long deposerAvecRelecteur() {
        presenceRepository.save(new Presence(session.getId(), 2L, Presence.Source.ETUDIANT));
        return exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
    }

    @Test
    void noteEtCommentaireVisiblesDesQuIlsSontRendus() { // EF8
        Long id = deposerAvecRelecteur();
        Relecture relecture = relectureRepository.findByExerciceId(id).orElseThrow();
        relectureService.rendre(relecture.getId(),
                new RendreRelectureRequest(BigDecimal.valueOf(14), "Bon travail dans l'ensemble."));

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.note()).isEqualTo(14);
        assertThat(detail.commentaire()).isEqualTo("Bon travail dans l'ensemble.");
        assertThat(detail.statut()).isEqualTo("NOTE");
        assertThat(detail.lien()).isEqualTo(LIEN);
        assertThat(detail.etudiantId()).isEqualTo(1L);
        assertThat(detail.sessionId()).isEqualTo(session.getId());
    }

    @Test
    void noteEtCommentaireRestentNulsAvantLeRendu() { // EF8
        Long id = deposerAvecRelecteur(); // relecteur assigné, mais rien de rendu

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(detail.note()).isNull();
        assertThat(detail.commentaire()).isNull();
    }

    @Test
    void laConsultationFonctionneSansAucuneRelecture() {
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.statut()).isEqualTo("DEPOSE");
        assertThat(detail.note()).isNull();
        assertThat(detail.commentaire()).isNull();
    }

    @Test
    void laConsultationNeFuitJamaisLIdentiteDuRelecteur() { // RG7
        Long id = deposerAvecRelecteur();
        Relecture relecture = relectureRepository.findByExerciceId(id).orElseThrow();
        relectureService.rendre(relecture.getId(),
                new RendreRelectureRequest(BigDecimal.valueOf(14), "OK"));
        assertThat(relecture.getRelecteurId()).isNotNull(); // la donnée existe bien en base

        // Aucun champ du DTO ne doit permettre d'identifier le relecteur
        assertThat(Arrays.stream(ExerciceDetailResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("relecteurId", "relecteur", "relecteurNom", "idRelecteur");
    }

    @Test
    void laConsultationRefuseePourUnExerciceInconnu() {
        assertThatThrownBy(() -> exerciceService.consulter(999999L))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("EXERCICE_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

}