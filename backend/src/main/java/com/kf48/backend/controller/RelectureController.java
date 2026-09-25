package com.kf48.backend.controller;

import com.kf48.backend.dto.CorrigerNoteRequest;
import com.kf48.backend.dto.RelectureResponse;
import com.kf48.backend.dto.RendreRelectureRequest;
import com.kf48.backend.service.RelectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Relectures", description = "Notation des exercices assignés")
@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @Operation(
            summary = "Rendre sa note et son commentaire",
            description = "EF6 : le relecteur note et commente l'exercice qui lui est assigné. "
                    + "RG8 : la note est un entier de 0 à 20. "
                    + "RG5 : un seul relecteur par exercice. "
                    + "RG7 : l'identité du relecteur n'est jamais exposée dans la réponse. "
                    + "La rendu est définitive : une seconde tentative renvoie 409.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relecture enregistrée"),
                    @ApiResponse(responseCode = "400", description = "NOTE_INVALIDE ou CHAMP_MANQUANT"),
                    @ApiResponse(responseCode = "403", description = "AUTO_RELECTURE"),
                    @ApiResponse(responseCode = "404", description = "RELECTURE_INCONNUE"),
                    @ApiResponse(responseCode = "409", description = "RELECTURE_DEJA_RENDUE")
            }
    )
    @PostMapping("/{id}")
    public ResponseEntity<RelectureResponse> rendre(@PathVariable Long id,
                                                    @Valid @RequestBody RendreRelectureRequest requete) {
        return ResponseEntity.ok(relectureService.rendre(id, requete));
    }

    @Operation(
            summary = "Corriger une note déjà envoyée",
            description = "EF7 : le relecteur corrige sa note et son commentaire. "
                    + "RG9 : accepté tant que la session n'est pas clôturée ; après clôture, la note "
                    + "est figée définitivement (409 SESSION_CLOTUREE). "
                    + "RG8 : la nouvelle note reste un entier de 0 à 20. "
                    + "La date de première rendu (rendueAt) n'est pas modifiée.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relecture corrigée"),
                    @ApiResponse(responseCode = "400", description = "NOTE_INVALIDE ou CHAMP_MANQUANT"),
                    @ApiResponse(responseCode = "404", description = "RELECTURE_INCONNUE"),
                    @ApiResponse(responseCode = "409", description = "SESSION_CLOTUREE")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<RelectureResponse> corriger(@PathVariable Long id,
                                                      @Valid @RequestBody CorrigerNoteRequest requete) {
        return ResponseEntity.ok(relectureService.corriger(id, requete));
    }
}