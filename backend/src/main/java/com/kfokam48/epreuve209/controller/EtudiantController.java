package com.kfokam48.epreuve209.controller;

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
 *
 * Fix v1 : on requête directement la table etudiant via EtudiantRepository.findByPromotionId()
 * au lieu de passer par promotion.getEtudiants() (relation LAZY hors transaction →
 * LazyInitializationException en conteneur, open-in-view=false).
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
        // Vérifie que la promotion existe — lève 404 PROMOTION_INCONNUE si absente
        if (!promotions.existsById(promotionId)) {
            throw new PromotionInconnueException(promotionId, HttpStatus.NOT_FOUND);
        }
        // Requête directe sans navigation LAZY : EtudiantRepository.findByPromotionId
        return etudiants.findByPromotionId(promotionId).stream()
                .map(e -> new EtudiantVue(e.getId(), e.getNom()))
                .toList();
    }
}
