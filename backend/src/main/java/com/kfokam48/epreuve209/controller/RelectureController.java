package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.NoterRelectureRequest;
import com.kfokam48.epreuve209.dto.RelectureResponse;
import com.kfokam48.epreuve209.service.RelectureService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService service;

    public RelectureController(RelectureService service) {
        this.service = service;
    }

    /**
     * POST = création de la relecture (opération imposée, 200).
     * `relecteurId` en query (extension documentée au contrat) : sans authentification
     * (Q1), l'identité voyage côté client — c'est ce qui rend l'erreur imposée
     * 403 AUTO_RELECTURE vérifiable.
     */
    @PostMapping("/{id}")
    public ResponseEntity<RelectureResponse> rendre(@PathVariable Long id,
                                                    @RequestParam Long relecteurId,
                                                    @Valid @RequestBody NoterRelectureRequest requete) {
        return ResponseEntity.ok(service.rendre(id, relecteurId, requete));
    }

    /** PATCH = correction avant clôture (extension : Q10/EF8/RG9). */
    @PatchMapping("/{id}")
    public ResponseEntity<RelectureResponse> corriger(@PathVariable Long id,
                                                      @RequestParam Long relecteurId,
                                                      @Valid @RequestBody NoterRelectureRequest requete) {
        return ResponseEntity.ok(service.corriger(id, relecteurId, requete));
    }
}
