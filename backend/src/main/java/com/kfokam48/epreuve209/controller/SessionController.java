package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.OuvrirSessionRequest;
import com.kfokam48.epreuve209.dto.SessionResponse;
import com.kfokam48.epreuve209.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> ouvrir(@Valid @RequestBody OuvrirSessionRequest requete) {
        SessionResponse session = service.ouvrirSession(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
}
