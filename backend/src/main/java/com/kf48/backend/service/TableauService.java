package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.LigneTableauResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.EtudiantRepository;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.PromotionRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EF11 : le tableau récapitulatif d'une promotion, une ligne par étudiant.
 *
 * Le périmètre est la promotion : ses étudiants, et les sessions de cette promotion.
 * RG15 : la moyenne porte sur les seuls exercices notés, toutes sessions confondues —
 * pas seulement la dernière séance. RG10 : les relectures non rendues apparaissent
 * dans « relecturesEnAttente », qui est la charge de travail qui reste à l'étudiant.
 */
@Service
public class TableauService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;
    private final SessionRepository sessionRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;

    public TableauService(PromotionRepository promotionRepository,
                          EtudiantRepository etudiantRepository,
                          SessionRepository sessionRepository,
                          PresenceRepository presenceRepository,
                          ExerciceRepository exerciceRepository,
                          RelectureRepository relectureRepository) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
        this.sessionRepository = sessionRepository;
        this.presenceRepository = presenceRepository;
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
    }

    @Transactional(readOnly = true)
    public List<LigneTableauResponse> consulter(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE");
        }

        List<Long> sessionIds = sessionRepository.findByPromotionId(promotionId).stream()
                .map(SessionCours::getId)
                .toList();

        // Une promotion peut n'avoir encore aucune session : le tableau est alors vide,
        // et non une erreur. Les requêtes d'agrégation sont donc court-circuitées.
        Map<Long, Long> presences = indexer(presenceRepository.compterParEtudiant(sessionIds));
        Map<Long, Long> exercices = indexer(exerciceRepository.compterParEtudiant(sessionIds));
        Map<Long, Long> enAttente = indexer(relectureRepository.compterEnAttenteParRelecteur(sessionIds));
        Map<Long, Double> moyennes = indexerMoyennes(exerciceRepository.moyenneNoteeParEtudiant(sessionIds));

        return etudiantRepository.findByPromotionIdOrderById(promotionId).stream()
                .map(etudiant -> new LigneTableauResponse(
                        etudiant.getId(),
                        etudiant.getNom(),
                        presences.getOrDefault(etudiant.getId(), 0L),
                        exercices.getOrDefault(etudiant.getId(), 0L),
                        moyennes.get(etudiant.getId()), // null si jamais noté (RG15)
                        enAttente.getOrDefault(etudiant.getId(), 0L)))
                .toList();
    }

    /** Transforme des couples (clé, valeur) en table de correspondance. */
    private static Map<Long, Long> indexer(List<Object[]> resultats) {
        Map<Long, Long> index = new HashMap<>();
        for (Object[] ligne : resultats) {
            index.put((Long) ligne[0], ((Number) ligne[1]).longValue());
        }
        return index;
    }

    private static Map<Long, Double> indexerMoyennes(List<Object[]> resultats) {
        Map<Long, Double> index = new HashMap<>();
        for (Object[] ligne : resultats) {
            index.put((Long) ligne[0], ((Number) ligne[1]).doubleValue());
        }
        return index;
    }
}