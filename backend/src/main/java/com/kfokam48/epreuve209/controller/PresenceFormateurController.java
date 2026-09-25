package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.PresenceResponse;
import com.kfokam48.epreuve209.service.PresenceFormateurService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/presences")
public class PresenceFormateurController {

    private final PresenceFormateurService service;

    public PresenceFormateurController(PresenceFormateurService service) {
        this.service = service;
    }

    public record CorpsPresenceFormateur(@NotNull(message = "La session est requise") Long sessionId,
                                         @NotNull(message = "L'étudiant est requis") Long etudiantId) {}

    @PostMapping("/formateur")
    public ResponseEntity<PresenceResponse> ajouter(@RequestBody @jakarta.validation.Valid CorpsPresenceFormateur corps) {
        PresenceResponse presence = service.ajouter(corps.sessionId(), corps.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(presence);
    }
}
