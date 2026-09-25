package com.kf48.backend.controller;

import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionClotureResponse;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sessions", description = "Ouverture des sessions de cours")
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(
            summary = "Ouvrir une session de cours",
            description = "Crée une session valable 15 minutes à partir de son ouverture (RG1) avec un code unique (RG17). Retourne 201 et les informations d'ouverture."
    )
    @PostMapping
    public ResponseEntity<SessionResponse> ouvrir(@Valid @RequestBody OuvrirSessionRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.ouvrir(requete));
    }

    @Operation(
            summary = "Clôturer une session",
            description = "EF9 : le formateur clôture explicitement une session. RG14 : la clôture est "
                    + "distincte de l'expiration automatique du code au bout de 15 minutes. "
                    + "Après clôture, les dépôts d'exercice, les remplacements de lien et les corrections "
                    + "de note sont refusés en 409 SESSION_CLOTUREE. "
                    + "RG10 : les exercices sans relecture rendue restent visibles comme « en attente ».",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session clôturée"),
                    @ApiResponse(responseCode = "404", description = "SESSION_INCONNUE"),
                    @ApiResponse(responseCode = "409", description = "SESSION_DEJA_CLOTUREE")
            }
    )
    @PostMapping("/{id}/cloture")
    public ResponseEntity<SessionClotureResponse> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.cloturer(id));
    }
}