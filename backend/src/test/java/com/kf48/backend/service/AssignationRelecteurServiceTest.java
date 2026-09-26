package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceResponse;
import com.kf48.backend.dto.MarquerPresenceRequest;
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

/**
 * RG4 / RG5 (révisée) / RG6 : l'assignation automatique de DEUX relecteurs.
 *
 * Le point délicat est le double declencheur (dépôt ET présence) : chaque cas
 * vérifie donc par quel chemin l'exercice a été assigné. Depuis le changement de
 * besoin (issue #27), un exercice ne reçoit plus UN relecteur mais DEUX distincts,
 * ce qui change trois choses : le nombre de relectures attendues, la possibilité
 * qu'un exercice soit PARTIELLEMENT assigné, et le besoin de compléter ensuite.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssignationRelecteurServiceTest {

    private static final String LIEN = "https://github.com/etudiant/exercice-1";

    @Autowired private ExerciceService exerciceService;
    @Autowired private PresenceService presenceService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private PresenceRepository presenceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private SessionCours session;

    @BeforeEach
    void ouvrirSession() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        session = sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)));
    }

    /** Enregistre directement une presence, sans passer par le code de session. */
    private void marcarPresent(Long etudiantId) {
        presenceRepository.save(new Presence(session.getId(), etudiantId, Presence.Source.ETUDIANT));
    }

    private Exercice depoter(Long etudiantId) {
        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), etudiantId, LIEN));
        return exerciceRepository.findById(reponse.id()).orElseThrow();
    }

    /** Relectures d'un exercice : il y en a deux dès que c'est complet. */
    private List<Relecture> relectures(Long exerciceId) {
        return relectureRepository.findByExerciceIdIn(List.of(exerciceId));
    }

    @Test
    void aucunPresentLExerciceResteDepose() { // RG6 : pas de relecteur disponible
        Exercice exercice = depoter(1L);

        assertThat(exercice.getStatut()).isEqualTo(Exercice.Statut.DEPOSE);
        assertThat(relectures(exercice.getId())).isEmpty();
    }

    @Test
    void deuxRelecteursSontAssignesAuDepotQuandDeuxAutresSontPresents() {
        marcarPresent(2L);
        marcarPresent(3L);

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        // L'assignation est tentée avant le retour : la réponse reflète l'état réel
        assertThat(reponse.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(relectures(reponse.id()))
                .extracting(Relecture::getRelecteurId)
                .containsExactlyInAnyOrder(2L, 3L);
        assertThat(relectures(reponse.id()))
                .allMatch(r -> r.getRendueAt() == null); // assignés, pas encore rendus
    }

    @Test
    void leRelecteurNEstJamaisLAuteur() { // RG4 : l'auto-relecture est impossible
        marcarPresent(1L); // seul l'auteur est présent

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        assertThat(reponse.statut()).isEqualTo("DEPOSE");
        assertThat(relectures(reponse.id())).isEmpty();
    }

    @Test
    void deuxRelecteursDistinctsSontTiresParmisLesPresents() { // RG5 révisée
        marcarPresent(2L);
        marcarPresent(3L);
        marcarPresent(4L);

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        List<Relecture> tirees = relectures(reponse.id());
        assertThat(tirees).hasSize(2);
        // Deux pairs DIFFÉRENTS, tous deux parmi les présents, jamais l'auteur (1)
        assertThat(tirees.get(0).getRelecteurId()).isNotEqualTo(tirees.get(1).getRelecteurId());
        assertThat(tirees).allMatch(r -> r.getRelecteurId() != 1L
                && (r.getRelecteurId() == 2L || r.getRelecteurId() == 3L || r.getRelecteurId() == 4L));
    }

    @Test
    void uneSeulePresenceNeSuffitPasACompleterLesDeuxRelecteurs() { // RG5 révisée + RG6
        marcarPresent(2L); // un seul autre étudiant présent

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        // Un seul relecteur possible : l'exercice reste en attente du second (RG6)
        assertThat(relectures(reponse.id()))
                .extracting(Relecture::getRelecteurId)
                .containsExactly(2L);
        assertThat(exerciceRepository.findById(reponse.id()).orElseThrow().getStatut())
                .isEqualTo(Exercice.Statut.EN_ATTENTE_RELECTURE);
    }

    @Test
    void unePresenceQuiArriveApresLaCompleteAvecUnSecondRelecteurDifferent() { // RG5 révisée + RG6
        marcarPresent(2L);
        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        assertThat(relectures(reponse.id())).hasSize(1);

        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L)); // nouvel arrivant

        // L'exercice incomplet est complété, et le second n'est pas le premier
        assertThat(relectures(reponse.id()))
                .extracting(Relecture::getRelecteurId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void aucunTroisiemeRelecteurNestAjouteApresLeSecond() { // RG5 révisée : au plus deux
        marcarPresent(2L);
        marcarPresent(3L);
        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        assertThat(relectures(reponse.id())).hasSize(2);

        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 4L));

        // L'exercice est complet : le nouvel arrivant ne s'y voit pas ajouter
        assertThat(relectures(reponse.id())).hasSize(2);
    }

    @Test
    void deuxExercicesSontAssignesADesRelecteursDistinctsDeLEurAuteur() { // RG4 + RG5 révisée
        marcarPresent(2L);
        marcarPresent(3L);
        marcarPresent(4L);
        ExerciceResponse premier = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        ExerciceResponse second = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 3L, LIEN));

        assertThat(premier.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(second.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(relectureRepository.count()).isEqualTo(4); // 2 par exercice
        for (Relecture relecture : relectureRepository.findAll()) {
            // L'étudiant 3 est l'auteur du second exercice : il ne peut pas le relire,
            // mais il peut relire le premier.
            Long auteur = relecture.getExerciceId().equals(premier.id()) ? 1L : 3L;
            assertThat(relecture.getRelecteurId()).isNotEqualTo(auteur);
        }
    }
}
