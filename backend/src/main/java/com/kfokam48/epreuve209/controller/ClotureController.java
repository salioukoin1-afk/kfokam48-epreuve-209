package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.ClotureResponse;
import com.kfokam48.epreuve209.service.ClotureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class ClotureController {

    private final ClotureService service;

    public ClotureController(ClotureService service) {
        this.service = service;
    }

    /** POST /api/sessions/{id}/cloture — extension du contrat (EF11, US-08). */
    @PostMapping("/{id}/cloture")
    public ResponseEntity<ClotureResponse> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(service.cloturer(id));
    }
}
