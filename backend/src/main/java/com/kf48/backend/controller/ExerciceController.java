package com.kf48.backend.controller;

import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceDetailResponse;
import com.kf48.backend.dto.ExerciceResponse;
import com.kf48.backend.dto.RemplacerLienRequest;
import com.kf48.backend.service.ExerciceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exercices", description = "Dépôt des exercices par les étudiants")
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @Operation(
            summary = "Déposer le lien de son exercice",
            description = "RG11 : le dépôt reste possible après l'expiration du code de présence (15 min), "
                    + "tant que la session n'est pas explicitement clôturée. RG19 : un seul dépôt par étudiant et par session. "
                    + "Le statut initial est DEPOSE ; l'assignation d'un relecteur (RG6) arrive au ticket #6.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Exercice déposé"),
                    @ApiResponse(responseCode = "400", description = "LIEN_INVALIDE ou CHAMP_MANQUANT"),
                    @ApiResponse(responseCode = "404", description = "SESSION_INCONNUE"),
                    @ApiResponse(responseCode = "409", description = "EXERCICE_DEJA_DEPOSE ou SESSION_CLOTUREE")
            }
    )
    @PostMapping
    public ResponseEntity<ExerciceResponse> deposer(@Valid @RequestBody DeposerExerciceRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciceService.deposer(requete));
    }

    @Operation(
            summary = "Remplacer le lien de son exercice",
            description = "RG12 : le remplacement est accepté tant qu'aucun relecteur n'est assigné, et refusé "
                    + "dès qu'un relecteur l'est — même si la relecture n'a pas encore été rendue. "
                    + "La colonne modifie_at est mise à jour.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lien remplacé"),
                    @ApiResponse(responseCode = "400", description = "LIEN_INVALIDE ou CHAMP_MANQUANT"),
                    @ApiResponse(responseCode = "404", description = "EXERCICE_INCONNU"),
                    @ApiResponse(responseCode = "409", description = "RELECTEUR_DEJA_ASSIGNE")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<ExerciceResponse> remplacerLien(@PathVariable Long id,
                                                           @Valid @RequestBody RemplacerLienRequest requete) {
        return ResponseEntity.ok(exerciceService.remplacerLien(id, requete));
    }

    @Operation(
            summary = "Consulter sa note et son commentaire",
            description = "EF8 : l'étudiant relu consulte le détail de son exercice. "
                    + "RG7 : l'identité du relecteur n'apparaît jamais dans la réponse. "
                    + "note et commentaire valent null tant que la relecture n'est pas rendue. "
                    + "La réponse suit exactement le schéma du contrat.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Détail de l'exercice"),
                    @ApiResponse(responseCode = "404", description = "EXERCICE_INCONNUE")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ExerciceDetailResponse> consulter(@PathVariable Long id) {
        return ResponseEntity.ok(exerciceService.consulter(id));
    }
}