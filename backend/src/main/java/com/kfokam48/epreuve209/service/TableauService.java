package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.LigneTableau;
import com.kfokam48.epreuve209.entity.Promotion;
import com.kfokam48.epreuve209.exception.PromotionInconnueException;
import com.kfokam48.epreuve209.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EF10 — le tableau du formateur, calculé intégralement côté serveur (F3).
 * RG10 : un exercice dont la relecture n'a jamais été rendue compte dans
 * relecturesEnAttente du relecteur, jamais ignoré silencieusement.
 */
@Service
public class TableauService {

    private final PromotionRepository promotions;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public TableauService(PromotionRepository promotions,
                          PresenceRepository presences,
                          ExerciceRepository exercices,
                          RelectureRepository relectures) {
        this.promotions = promotions;
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<LigneTableau> tableau(Long promotionId) {
        Promotion promotion = promotions.findById(promotionId)
                .orElseThrow(() -> new PromotionInconnueException(promotionId, HttpStatus.NOT_FOUND));

        return promotion.getEtudiants().stream()
                .map(etudiant -> {
                    Long id = etudiant.getId();
                    int nbPresences = (int) presences.countByEtudiantId(id);
                    int nbExercices = (int) exercices.countByAuteurId(id);

                    // Moyenne des notes REÇUES (l'étudiant relu) — null si aucune note (Q16).
                    List<Integer> notesRecues = relectures.findNotesRecues(id);
                    Double moyenne = notesRecues.isEmpty()
                            ? null
                            : notesRecues.stream().mapToInt(Integer::intValue).average().orElse(0);

                    // RG10 : relectures dues = celles dont il est relecteur et jamais rendues.
                    int relecturesEnAttente = relectures.countEnAttenteParRelecteur(id);

                    return new LigneTableau(id, etudiant.getNom(), nbPresences,
                            nbExercices, moyenne, relecturesEnAttente);
                })
                .toList();
    }
}
