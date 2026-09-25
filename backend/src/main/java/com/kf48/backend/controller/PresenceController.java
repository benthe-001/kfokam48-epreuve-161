package com.kf48.backend.controller;

import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Présences", description = "Marquage de présence d'un étudiant")
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Operation(
            summary = "Marquer sa présence avec un code",
            description = "RG1 : le code expire 15 minutes après l'ouverture. RG18 : une présence unique par étudiant et par session. La source est toujours ETUDIANT (RG13)."
    )
    @PostMapping
    public ResponseEntity<PresenceResponse> marquer(@Valid @RequestBody MarquerPresenceRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenceService.marquer(requete));
    }
}