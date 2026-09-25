package com.kf48.relectures.controller;

import com.kfokam48.relectures.dto.OuvrirSessionRequest;
import com.kfokam48.relectures.dto.SessionResponse;
import com.kfokam48.relectures.service.SessionService;
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