package com.kf48.backend.controller;

import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
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
}