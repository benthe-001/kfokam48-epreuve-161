package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.CorrigerNoteRequest;
import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.RelectureResponse;
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

/** EF6 / RG5 / RG8 : le relecteur note et commente l'exercice qui lui est assigne. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RelectureServiceTest {

    private static final String LIEN = "https://github.com/etudiant/exercice-1";

    @Autowired private RelectureService relectureService;
    @Autowired private ExerciceService exerciceService;
    @Autowired private PresenceService presenceService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private PresenceRepository presenceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private SessionCours session;
    private Long exerciceId;

    /** Cree le scenario complet : l'etudiant 1 depose, l'etudiant 2 est tire comme relecteur. */
    @BeforeEach
    void assignerUneRelecture() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        session = sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)));
        presenceRepository.save(new Presence(session.getId(), 2L, Presence.Source.ETUDIANT));
        exerciceId = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN)).id();
    }

    private Relecture relecture() {
        return relectureRepository.findByExerciceId(exerciceId).orElseThrow();
    }

    private static RendreRelectureRequest rendre(int note) {
        return new RendreRelectureRequest(BigDecimal.valueOf(note), "Travail clair et complet.");
    }

    @Test
    void rendreEnregistreNoteCommentaireEtStatutNote() {
        RelectureResponse reponse = relectureService.rendre(relecture().getId(), rendre(15));

        assertThat(reponse.note()).isEqualTo(15);
        assertThat(reponse.commentaire()).isEqualTo("Travail clair et complet.");
        assertThat(reponse.rendueAt()).isNotNull();

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(Exercice.Statut.NOTE);
    }

    @Test
    void noteZeroAcceptee() { // RG8 : borne basse incluse
        relectureService.rendre(relecture().getId(), rendre(0));
        assertThat(relecture().getNote()).isZero();
    }

    @Test
    void noteVingtAcceptee() { // RG8 : borne haute incluse (2 chiffres)
        relectureService.rendre(relecture().getId(), rendre(20));
        assertThat(relecture().getNote()).isEqualTo(20);
    }

    @Test
    void relectureDejaRendueRenvoie409() {
        Long id = relecture().getId();
        relectureService.rendre(id, rendre(12));

        assertThatThrownBy(() -> relectureService.rendre(id, rendre(15)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RELECTURE_DEJA_RENDUE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });

        // La note rendue n'a pas ete ecrasee par la tentative refusee
        assertThat(relecture().getNote()).isEqualTo(12);
    }
    @Test
    void autoRelectureRenvoie403() {
        // RG4 rend ce cas impossible par l'assignation ; on le fabrique pour tester le garde-fou.
        // Il faut un AUTRE exercice et un AUTRE auteur : uk_relecture_exercice (RG5) interdit
        // deux relectures par exercice, et uk_exercice_session_etudiant (RG19) un seul dépôt
        // par etudiant et par session. L'etudiant 3 est donc l'auteur ET le relecteur.
        Long autreExercice = exerciceRepository.save(new Exercice(session.getId(), 3L, LIEN)).getId();
        Relecture fautive = relectureRepository.save(new Relecture(autreExercice, 3L));

        assertThatThrownBy(() -> relectureService.rendre(fautive.getId(), rendre(15)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("AUTO_RELECTURE");
                    assertThat(e.getStatut().value()).isEqualTo(403);
                });

        assertThat(fautive.getRendueAt()).isNull(); // rien n'a ete enregistre
    }

    @Test
    void relectureInconnueRenvoie404() {
        assertThatThrownBy(() -> relectureService.rendre(999999L, rendre(15)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RELECTURE_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    @Test
    void laReponseNExposePasLIdentiteDuRelecteur() { // RG7
        RelectureResponse reponse = relectureService.rendre(relecture().getId(), rendre(15));

        assertThat(Arrays.stream(RelectureResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("relecteurId", "relecteur");
    }

    @Test
    void laRelectureResteLieeAuRelecteurTireAuSort() { // RG5
        Long relecteurAssigne = relecture().getRelecteurId();
        assertThat(relecteurAssigne).isEqualTo(2L);

        relectureService.rendre(relecture().getId(), rendre(18));

        assertThat(relecture().getRelecteurId()).isEqualTo(relecteurAssigne);
    }

    @Test
    void uneNouvellePresenceNeReassignePasLExercice() {
        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L));

        Relecture apres = relecture();
        assertThat(apres.getRelecteurId()).isEqualTo(2L);
        assertThat(apres.getRendueAt()).isNull();
        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(Exercice.Statut.EN_ATTENTE_RELECTURE);
    }

    // ---- EF7 / RG9 : correction d'une note déjà rendue ----

    private static CorrigerNoteRequest corriger(int note) {
        return new CorrigerNoteRequest(BigDecimal.valueOf(note), "Note corrigée après relecture.");
    }

    /** Rend la relecture du @BeforeEach pour pouvoir la corriger ensuite. */
    private void rendreLaRelecture(int note) {
        relectureService.rendre(relecture().getId(), rendre(note));
    }

    @Test
    void correctionAccepteeTantQueLaSessionEstOuverte() { // RG9
        rendreLaRelecture(10);

        RelectureResponse reponse = relectureService.corriger(relecture().getId(), corriger(16));

        assertThat(reponse.note()).isEqualTo(16);
        assertThat(reponse.commentaire()).isEqualTo("Note corrigée après relecture.");
        assertThat(relecture().getNote()).isEqualTo(16);
    }

    @Test
    void laCorrectionNeChangePasLaDateDeRendu() {
        rendreLaRelecture(10);
        var premierRendu = relecture().getRendueAt();

        relectureService.corriger(relecture().getId(), corriger(16));

        assertThat(relecture().getRendueAt()).isEqualTo(premierRendu);
    }

    @Test
    void correctionRefuseeApresClotureDeLaSession() { // RG9 : la note est figée
        rendreLaRelecture(10);
        session.cloturer(); // RG14 : clôture explicite du formateur

        assertThatThrownBy(() -> relectureService.corriger(relecture().getId(), corriger(16)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_CLOTUREE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });

        assertThat(relecture().getNote()).isEqualTo(10); // inchangée
    }

    @Test
    void laClotureEstVerifieeAvantLeRenduDeLaRelecture() {
        // Une session clôturée refuse aussi la correction d'une relecture jamais rendue :
        // l'ordre des contrôles place RG9 avant le contrôle de rendu.
        session.cloturer();

        assertThatThrownBy(() -> relectureService.corriger(relecture().getId(), corriger(16)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("SESSION_CLOTUREE"));
    }

    @Test
    void corrigerUneRelectureJamaisRendueRenvoie404() {
        assertThat(relecture().getRendueAt()).isNull();

        assertThatThrownBy(() -> relectureService.corriger(relecture().getId(), corriger(16)))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RELECTURE_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    @Test
    void corrigerUneRelectureInconnueRenvoie404() {
        assertThatThrownBy(() -> relectureService.corriger(999999L, corriger(16)))
                .isInstanceOfSatisfying(MetierException.class, e ->
                        assertThat(e.getCode()).isEqualTo("RELECTURE_INCONNUE"));
    }

    @Test
    void laCorrectionLaisseLExerciceAuStatutNote() {
        rendreLaRelecture(10);
        relectureService.corriger(relecture().getId(), corriger(16));

        assertThat(exerciceRepository.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(Exercice.Statut.NOTE);
    }
}
