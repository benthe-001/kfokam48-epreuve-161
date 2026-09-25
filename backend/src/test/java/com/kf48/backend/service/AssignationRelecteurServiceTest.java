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
 * RG4 / RG5 / RG6 : l'assignation automatique d'un relecteur.
 * Le point delicat est le double declencheur (depot ET presence) : chaque cas
 * verifie donc par quel chemin l'exercice a ete assigne.
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
    private void marquerPresent(Long etudiantId) {
        presenceRepository.save(new Presence(session.getId(), etudiantId, Presence.Source.ETUDIANT));
    }

    private Exercice depoter(Long etudiantId) {
        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), etudiantId, LIEN));
        return exerciceRepository.findById(reponse.id()).orElseThrow();
    }

    @Test
    void aucunPresentLExerciceResteDepose() { // RG6 : pas de relecteur disponible
        Exercice exercice = depoter(1L);

        assertThat(exercice.getStatut()).isEqualTo(Exercice.Statut.DEPOSE);
        assertThat(relectureRepository.findByExerciceId(exercice.getId())).isEmpty();
    }

    @Test
    void unAutrePresentAssigneUnRelecteurAuDepot() { // RG6, declencheur 1 : le depot
        marquerPresent(2L);

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        // L'assignation est tentee avant le retour : la reponse reflete l'etat reel
        assertThat(reponse.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        Relecture relecture = relectureRepository.findByExerciceId(reponse.id()).orElseThrow();
        assertThat(relecture.getRelecteurId()).isEqualTo(2L);
        assertThat(relecture.getRendueAt()).isNull(); // assignee, pas encore rendue
    }

    @Test
    void leRelecteurNEstJamaisLAuteur() { // RG4 : l'auto-relecture est impossible
        marquerPresent(1L); // seul l'auteur est present

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        assertThat(reponse.statut()).isEqualTo("DEPOSE");
        assertThat(relectureRepository.findByExerciceId(reponse.id())).isEmpty();
    }

    @Test
    void relecteurTireParmisTousLesPresents() { // RG6 : tirage au sort, pas le premier
        marquerPresent(2L);
        marquerPresent(3L);
        marquerPresent(4L);

        ExerciceResponse reponse = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));

        Relecture relecture = relectureRepository.findByExerciceId(reponse.id()).orElseThrow();
        assertThat(relecture.getRelecteurId()).isIn(2L, 3L, 4L);
    }

    @Test
    void uneNouvellePresenceRetenteLesExercicesEnAttente() { // RG6, declencheur 2 : la presence
        Exercice exercice = depoter(1L); // personne n'est present : reste DEPOSE
        assertThat(exercice.getStatut()).isEqualTo(Exercice.Statut.DEPOSE);

        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 2L));

        // L'arrivee du deuxieme etudiant a debloque l'exercice deja depose
        Relecture relecture = relectureRepository.findByExerciceId(exercice.getId()).orElseThrow();
        assertThat(relecture.getRelecteurId()).isEqualTo(2L);
        assertThat(exerciceRepository.findById(exercice.getId()).orElseThrow().getStatut())
                .isEqualTo(Exercice.Statut.EN_ATTENTE_RELECTURE);
    }

    @Test
    void unExerciceDejaAssigneNaPasDeSecondRelecteur() { // RG5
        marquerPresent(2L);
        marquerPresent(3L);
        ExerciceResponse premiere = exerciceService.deposer(
                new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        Long relecteurInitial = relectureRepository.findByExerciceId(premiere.id()).orElseThrow().getRelecteurId();

        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 4L)); // nouvel arrivant

        List<Relecture> relectures = relectureRepository.findAll().stream()
                .filter(r -> r.getExerciceId().equals(premiere.id()))
                .toList();
        assertThat(relectures).hasSize(1);
        assertThat(relectures.get(0).getRelecteurId()).isEqualTo(relecteurInitial);
    }

    @Test
    void deuxExercicesSontAssignesAUnRelecteurDifferentDeLEurAuteur() { // RG4 + RG5
        marquerPresent(2L);
        ExerciceResponse premier = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 1L, LIEN));
        ExerciceResponse second = exerciceService.deposer(new DeposerExerciceRequest(session.getId(), 3L, LIEN));

        assertThat(premier.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(second.statut()).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(relectureRepository.count()).isEqualTo(2);
        for (Relecture relecture : relectureRepository.findAll()) {
            Long auteur = relecture.getExerciceId().equals(premier.id()) ? 1L : 3L;
            assertThat(relecture.getRelecteurId()).isNotEqualTo(auteur);
        }
    }
}
