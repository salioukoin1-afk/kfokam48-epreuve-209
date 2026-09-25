package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.MarquerPresenceRequest;
import com.kfokam48.epreuve209.dto.PresenceResponse;
import com.kfokam48.epreuve209.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PresenceResponse> marquer(@Valid @RequestBody MarquerPresenceRequest requete) {
        PresenceResponse presence = service.marquerPresent(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(presence);
    }
}
