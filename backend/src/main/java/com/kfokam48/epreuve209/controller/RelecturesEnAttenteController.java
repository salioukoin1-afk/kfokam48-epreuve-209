package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.RelecturesEnAttenteResponse;
import com.kfokam48.epreuve209.repository.RelectureRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EF6/RG10 — la liste « Exercices à relire » d'un relecteur (extension du contrat v1.1).
 * Une relecture jamais rendue reste listée même après clôture de session : c'est le
 * « en attente » que le client veut voir clairement (Q11).
 */
@RestController
@RequestMapping("/api/relectures")
public class RelecturesEnAttenteController {

    private final RelectureRepository relectures;

    public RelecturesEnAttenteController(RelectureRepository relectures) {
        this.relectures = relectures;
    }

    public interface VueEnAttente {
        Long getExerciceId();
        Long getRelectureId();
        Long getExerciceSessionId();
        String getExerciceSessionTitre();
        String getExerciceLien();
        String getExerciceAuteurNom();
    }

    @GetMapping("/en-attente")
    public List<RelecturesEnAttenteResponse> enAttente(@RequestParam Long etudiantId) {
        return relectures.findByRelecteurIdAndRendueAtIsNull(etudiantId).stream()
                .map(r -> new RelecturesEnAttenteResponse(
                        r.getExercice().getId(),
                        r.getId(),
                        r.getExercice().getSession().getId(),
                        r.getExercice().getSession().getTitre(),
                        r.getExercice().getLien(),
                        r.getExercice().getAuteur().getNom()))
                .toList();
    }
}
