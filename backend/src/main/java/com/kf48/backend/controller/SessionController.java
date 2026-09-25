package com.kf48.backend.controller;

import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> ouvrir(@Valid @RequestBody OuvrirSessionRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.ouvrir(requete));
    }
}