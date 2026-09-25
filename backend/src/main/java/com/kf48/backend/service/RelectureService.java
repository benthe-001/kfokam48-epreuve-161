package com.kf48.backend.service;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.dto.RelectureResponse;
import com.kf48.backend.dto.RendreRelectureRequest;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.RelectureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelectureService {

    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;

    public RelectureService(RelectureRepository relectureRepository,
                            ExerciceRepository exerciceRepository) {
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
    }

    /**
     * EF6 : le relecteur assigné rend sa note et son commentaire.
     * RG8 : note entière 0–20 (validée sur le DTO, avant d'arriver ici).
     * RG5 : la relecture porte sur l'exercice assigné, un seul relecteur par exercice.
     *
     * Le relecteur n'est pas fourni dans la requête : il est celui de l'assignation
     * réalisée au ticket #6 (le projet n'a pas encore d'authentification, cf. absence
     * de spring-boot-starter-security). L'auto-relecture est donc un garde-fou :
     * elle ne devrait jamais se produire puisque RG4 exclut l'auteur des candidats,
     * mais elle est vérifiée pour protéger la règle même en cas de donnée incohérente.
     */
    @Transactional
    public RelectureResponse rendre(Long relectureId, RendreRelectureRequest requete) {
        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE"));

        Exercice exercice = exerciceRepository.findById(relecture.getExerciceId())
                .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "EXERCICE_INCONNUE"));

        if (relecture.getRelecteurId().equals(exercice.getEtudiantId())) { // RG4 : auto-relecture interdite
            throw new MetierException(HttpStatus.FORBIDDEN, "AUTO_RELECTURE");
        }

        if (relecture.getRendueAt() != null) { // une relecture rendue est définitive
            throw new MetierException(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE");
        }

        relecture.rendre(requete.note().intValueExact(), requete.commentaire());
        exercice.marquerNote(); // l'exercice passe à NOTE

        // Les deux entités sont managées : persistées par dirty checking en fin de transaction.
        return RelectureResponse.depuis(relecture);
    }
}