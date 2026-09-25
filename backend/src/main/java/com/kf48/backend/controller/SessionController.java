package com.kf48.backend.controller;

import com.kf48.backend.dto.AjouterPresenceRequest;
import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.dto.SessionClotureResponse;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.service.PresenceService;
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
    private final PresenceService presenceService;

    public SessionController(SessionService sessionService, PresenceService presenceService) {
        this.sessionService = sessionService;
        this.presenceService = presenceService;
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

    @Operation(
            summary = "Ajouter une présence manuellement",
            description = "EF10 : le formateur ajoute une présence pour un étudiant de cette session. "
                    + "RG13 : la source est FORMATEUR (et non ETUDIANT) ; l'opération reste possible "
                    + "après l'expiration du code de présence — un formateur doit pouvoir rattraper un "
                    + "étudiant oublié — mais elle est refusée après la clôture de la session. "
                    + "RG18 : un étudiant déjà présent ne peut pas être ajouté deux fois.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Présence ajoutée manuellement"),
                    @ApiResponse(responseCode = "400", description = "ETUDIANT_INCONNU ou CHAMP_MANQUANT"),
                    @ApiResponse(responseCode = "404", description = "SESSION_INCONNUE"),
                    @ApiResponse(responseCode = "409", description = "DEJA_PRESENT ou SESSION_CLOTUREE")
            }
    )
    @PostMapping("/{id}/presences")
    public ResponseEntity<PresenceResponse> ajouterPresence(@PathVariable Long id,
                                                           @Valid @RequestBody AjouterPresenceRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presenceService.ajouterManuellement(id, requete));
    }
}