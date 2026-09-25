package com.kf48.backend.controller;

import com.kf48.backend.dto.DeposerExerciceRequest;
import com.kf48.backend.dto.ExerciceResponse;
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
}