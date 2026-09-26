package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
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
import java.util.Comparator;
import java.util.List;

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

    /** Depose un exercice ; les etudiants 2 et 3 sont presents pour les DEUX relecteurs (RG5). */
    private Long deposerAvecRelecteurs() {
        presenceRepository.save(new Presence(session.getId(), 2L, Presence.Source.ETUDIANT));
        presenceRepository.save(new Presence(session.getId(), 3L, Presence.Source.ETUDIANT));
        return exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
    }

    /** Les relecteurs de l'exercice, tries par identifiant pour la stabilite. */
    private List<Relecture> relectures(Long exerciceId) {
        return relectureRepository.findByExerciceIdIn(List.of(exerciceId)).stream()
                .sorted(Comparator.comparing(Relecture::getId))
                .toList();
    }

    private void rendre(Relecture relecture, int note, String commentaire) {
        relectureService.rendre(relecture.getId(),
                new RendreRelectureRequest(BigDecimal.valueOf(note), commentaire));
    }


    @Test
    void laNoteEstLaMoyenneDesDeuxRelecturesRendues() { // EF8, RG17
        Long id = deposerAvecRelecteurs();
        List<Relecture> relectures = relectures(id);
        rendre(relectures.get(0), 14, "Bon travail dans l'ensemble.");
        rendre(relectures.get(1), 18, "Clair et bien structure.");

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.note()).isEqualTo(16.0); // (14 + 18) / 2
        assertThat(detail.noteProvisoire()).isFalse();
        assertThat(detail.statut()).isEqualTo("NOTE");
        assertThat(detail.lien()).isEqualTo(LIEN);
        assertThat(detail.etudiantId()).isEqualTo(1L);
        assertThat(detail.sessionId()).isEqualTo(session.getId());
    }

    @Test
    void uneSeuleRelectureRendueDonneUneNoteProvisoire() { // RG18
        Long id = deposerAvecRelecteurs();
        List<Relecture> relectures = relectures(id);
        rendre(relectures.get(0), 16, "Premier retour.");

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        // La note du seul relecteur qui a rendu, mais marquée provisoire
        assertThat(detail.note()).isEqualTo(16.0);
        assertThat(detail.noteProvisoire()).isTrue();
        assertThat(detail.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
    }

    @Test
    void lesCommentairesDesDeuxPairsSontConcatenesSansIdentifierQuiLesEcrit() { // RG7
        Long id = deposerAvecRelecteurs();
        List<Relecture> relectures = relectures(id);
        rendre(relectures.get(0), 14, "Premier retour.");
        rendre(relectures.get(1), 18, "Second retour.");

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.commentaire()).contains("Premier retour.", "Second retour.");
    }

    @Test
    void noteEtCommentaireRestentNulsAvantLeRendu() { // EF8
        Long id = deposerAvecRelecteurs(); // relecteurs assignes, rien de rendu

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(detail.note()).isNull();
        // Sans aucune note, il n'y a rien de provisoire : le drapeau vaut false
        assertThat(detail.noteProvisoire()).isFalse();
        assertThat(detail.commentaire()).isNull();
    }

    @Test
    void laConsultationFonctionneSansAucuneRelecture() {
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();

        ExerciceDetailResponse detail = exerciceService.consulter(id);

        assertThat(detail.statut()).isEqualTo("DEPOSE");
        assertThat(detail.note()).isNull();
        assertThat(detail.noteProvisoire()).isFalse();
        assertThat(detail.commentaire()).isNull();
    }

    @Test
    void laConsultationNeFuitJamaisLIdentiteDuRelecteur() { // RG7
        Long id = deposerAvecRelecteurs();
        List<Relecture> relectures = relectures(id);
        rendre(relectures.get(0), 14, "OK");
        rendre(relectures.get(1), 18, "OK");
        assertThat(relectures).allMatch(r -> r.getRelecteurId() != null); // la donnée existe en base

        // Aucun champ du DTO ne doit permettre d'identifier le relecteur
        assertThat(Arrays.stream(ExerciceDetailResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("relecteurId", "relecteur", "relecteurNom", "idRelecteur");
    }

    @Test
    void laConsultationRefuseePourUnExerciceInconnu() {
        assertThatThrownBy(() -> exerciceService.consulter(999999L))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("EXERCICE_INCONNU");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    // ---- EF9 : apres cloture, plus rien ne bouge (RG14) ; RG10 : rien ne disparait ----

    @Test
    void depotRefuseApresClotureDeLaSession() { // RG11, RG14
        session.cloturer();

        assertThatThrownBy(() -> exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 2L, LIEN)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_CLOTUREE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });
    }

    @Test
    void remplacementDeLienRefuseApresClotureDeLaSession() { // RG11, RG14
        // Aucun relecteur assigné : sans ce contrôle, RG12 autoriserait le remplacement
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
        session.cloturer();

        assertThatThrownBy(() -> exerciceService.remplacerLien(
                id, new RemplacerLienRequest("https://github.com/etudiant/autre")))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_CLOTUREE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });

        assertThat(exerciceRepository.findById(id).orElseThrow().getLien()).isEqualTo(LIEN);
    }

    @Test
    void unExerciceSansRelectureRendueResteEnAttenteApresCloture() { // RG10
        presenceRepository.save(new Presence(session.getId(), 2L, Presence.Source.ETUDIANT));
        Long id = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
        session.cloturer();

        // La clôture ne doit rien changer à l'exercice : il reste assigné, non rendu, visible.
        Exercice apres = exerciceRepository.findById(id).orElseThrow();
        assertThat(apres.getStatut()).isEqualTo(Exercice.Statut.EN_ATTENTE_RELECTURE);
        assertThat(relectureRepository.findByExerciceId(id)).isPresent();

        // Et il reste consultable par l'étudiant, avec une note absente
        ExerciceDetailResponse detail = exerciceService.consulter(id);
        assertThat(detail.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(detail.note()).isNull();
        assertThat(detail.commentaire()).isNull();
    }


}
