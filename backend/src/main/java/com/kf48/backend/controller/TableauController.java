package com.kf48.backend.controller;

import com.kf48.backend.dto.LigneTableauResponse;
import com.kf48.backend.service.TableauService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tableau de bord", description = "Récapitulatif par étudiant pour le formateur")
@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @Operation(
            summary = "Consulter le tableau récapitulatif d'une promotion",
            description = "EF11 : une ligne par étudiant de la promotion, avec le nombre de présences, "
                    + "d'exercices déposés, sa moyenne et le nombre de relectures qu'il doit encore rendre. "
                    + "RG15 : la moyenne ne porte que sur les exercices notés, toutes sessions confondues, "
                    + "et vaut null si l'étudiant n'a aucun exercice noté. "
                    + "RG10 : les relectures non rendues restent visibles via relecturesEnAttente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Récapitulatif par étudiant"),
                    @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE")
            }
    )
    @GetMapping
    public ResponseEntity<List<LigneTableauResponse>> consulter(@RequestParam Long promotionId) {
        return ResponseEntity.ok(tableauService.consulter(promotionId));
    }
}