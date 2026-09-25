package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.MonExerciceResponse;
import com.kfokam48.epreuve209.entity.Exercice;
import com.kfokam48.epreuve209.entity.Relecture;
import com.kfokam48.epreuve209.exception.ExerciceInconnuException;
import com.kfokam48.epreuve209.repository.ExerciceRepository;
import com.kfokam48.epreuve209.repository.RelectureRepository;
import org.springframework.web.bind.annotation.*;

/**
 * EF9 — consultation de sa note par l'auteur (extension du contrat v1.1).
 * B3 : lecture directe via repositories, aucun accès concurrent d'écriture ;
 * RG7 : la réponse est construite champ à champ — aucune propriété du relecteur
 * ne peut s'y glisser, même par accident d'évolution future.
 */
@RestController
@RequestMapping("/api/exercices")
public class MonExerciceController {

    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public MonExerciceController(ExerciceRepository exercices, RelectureRepository relectures) {
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @GetMapping("/miens")
    public MonExerciceResponse monExercice(@RequestParam Long sessionId, @RequestParam Long etudiantId) {
        Exercice exercice = exercices.findBySessionIdAndAuteurId(sessionId, etudiantId)
                .orElseThrow(ExerciceInconnuException::new);

        Relecture relecture = relectures.findByExerciceId(exercice.getId()).orElse(null);
        boolean rendue = relecture != null && relecture.getRendueAt() != null;

        return new MonExerciceResponse(
                exercice.getId(),
                exercice.getStatut(),
                exercice.getLien(),
                rendue ? relecture.getNote() : null,
                rendue ? relecture.getCommentaire() : null);
    }
}
