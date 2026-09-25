package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.RelecturesEnAttenteResponse;
import com.kfokam48.epreuve209.repository.RelectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EF6/RG10 — la liste « Exercices à relire » d'un relecteur.
 * La navigation dans les associations LAZY (exercice → session → auteur) se fait
 * ICI, dans une transaction en lecture seule. Une première version plaçait ce code
 * dans le contrôleur : elle explosait en LazyInitializationException en conteneur
 * (open-in-view=false), sans être détectée par les tests MockMvc — la transaction
 * du test masquait le problème. Leçon : toute lecture d'associations vit dans un
 * service transactionnel.
 */
@Service
public class RelecturesEnAttenteService {

    private final RelectureRepository relectures;

    public RelecturesEnAttenteService(RelectureRepository relectures) {
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<RelecturesEnAttenteResponse> enAttente(Long etudiantId) {
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
