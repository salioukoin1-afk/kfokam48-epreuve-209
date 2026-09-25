package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.entity.Promotion;
import com.kfokam48.epreuve209.exception.PromotionInconnueException;
import com.kfokam48.epreuve209.repository.EtudiantRepository;
import com.kfokam48.epreuve209.repository.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Q1 — l'étudiant se choisit dans une liste (pas de mot de passe) : le frontend doit
 * pouvoir afficher cette liste. Lecture seule, aucune donnée sensible (id + nom).
 * 404 PROMOTION_INCONNUE, cohérent avec GET /api/tableau.
 */
@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    public record EtudiantVue(Long id, String nom) {}

    private final EtudiantRepository etudiants;
    private final PromotionRepository promotions;

    public EtudiantController(EtudiantRepository etudiants, PromotionRepository promotions) {
        this.etudiants = etudiants;
        this.promotions = promotions;
    }

    @GetMapping
    public List<EtudiantVue> liste(@RequestParam Long promotionId) {
        Promotion promotion = promotions.findById(promotionId)
                .orElseThrow(() -> new PromotionInconnueException(promotionId, HttpStatus.NOT_FOUND));
        return promotion.getEtudiants().stream()
                .map(e -> new EtudiantVue(e.getId(), e.getNom()))
                .toList();
    }
}
