package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.RelecturesEnAttenteResponse;
import com.kfokam48.epreuve209.service.RelecturesEnAttenteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EF6/RG10 — la liste « Exercices à relire » d'un relecteur (extension du contrat v1.1).
 * Une relecture jamais rendue reste listée même après clôture de session (Q11).
 */
@RestController
@RequestMapping("/api/relectures")
public class RelecturesEnAttenteController {

    private final RelecturesEnAttenteService service;

    public RelecturesEnAttenteController(RelecturesEnAttenteService service) {
        this.service = service;
    }

    @GetMapping("/en-attente")
    public List<RelecturesEnAttenteResponse> enAttente(@RequestParam Long etudiantId) {
        return service.enAttente(etudiantId);
    }
}
