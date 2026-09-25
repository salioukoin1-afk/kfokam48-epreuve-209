package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.NoterRelectureRequest;
import com.kfokam48.epreuve209.dto.RelectureResponse;
import com.kfokam48.epreuve209.entity.Exercice;
import com.kfokam48.epreuve209.entity.Relecture;
import com.kfokam48.epreuve209.exception.*;
import com.kfokam48.epreuve209.repository.ExerciceRepository;
import com.kfokam48.epreuve209.repository.RelectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Rendu (POST, EF7) et correction (PATCH, EF8/RG9) d'une relecture.
 *
 * RG4 : refus si relecteurId == auteurId (défense en profondeur — l'assignation ne
 * peut pas produire ce cas). RG5 : une seule relecture par exercice (UK4).
 * RG8 : note entière, bornes 0 et 20 incluses. RG9 (tranché Q10 vs Q15) : correction
 * possible tant que Session.clotureeAt est nul — le verrou porte sur la CLÔTURE DE
 * SESSION, pas sur le moment du rendu. POST = création stricte : un second POST sur
 * une relecture déjà rendue renvoie 409 RELECTURE_DEJA_RENDUE (imposé) ; la
 * correction passe par PATCH.
 */
@Service
public class RelectureService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures,
                            ExerciceRepository exercices,
                            Clock horloge) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.horloge = horloge;
    }

    /** EF7 — POST /api/relectures/{id} : création de la note (imposé par le contrat). */
    @Transactional
    public RelectureResponse rendre(Long relectureId, Long relecteurId, NoterRelectureRequest requete) {
        Relecture relecture = charger(relectureId);
        validerNote(requete.note());

        if (relecture.getExercice().getAuteur().getId().equals(relecteurId)) {
            throw new AutoRelectureException();                  // 403 RG4 — autorisation avant état
        }
        if (relecture.getRendueAt() != null) {
            throw new RelectureDejaRendueException();            // 409 imposé (POST = création stricte)
        }
        verifierSessionNonCloturee(relecture);                   // 409 SESSION_CLOTUREE

        appliquer(relecture, requete);
        relecture.setRendueAt(LocalDateTime.now(horloge));

        // Re-charge l'exercice comme instance managée (le champ LAZY de relecture est
        // un proxy : écrire dessus ne garantit pas le dirty checking).
        Exercice exercice = exercices.findById(relecture.getExercice().getId())
                .orElseThrow(() -> new RelectureInconnueException(relectureId));
        exercice.setStatut("RELU");                              // transition D4 : EN_RELECTURE → RELU
        exercices.save(exercice);

        return new RelectureResponse(relecture.getId(), exercice.getId(),
                relecture.getNote(), relecture.getCommentaire());
    }

    /** EF8/RG9 — PATCH /api/relectures/{id} : correction tant que la session n'est pas clôturée. */
    @Transactional
    public RelectureResponse corriger(Long relectureId, Long relecteurId, NoterRelectureRequest requete) {
        Relecture relecture = charger(relectureId);
        validerNote(requete.note());

        if (relecture.getRendueAt() == null) {
            throw new RelectureInconnueException(relectureId);   // corriger une relecture jamais rendue n'a pas de sens
        }
        if (relecture.getExercice().getAuteur().getId().equals(relecteurId)) {
            throw new AutoRelectureException();
        }
        verifierSessionNonCloturee(relecture);                   // RG9 : le seul verrou autorisé

        appliquer(relecture, requete);
        relecture.setMajAt(LocalDateTime.now(horloge));          // trace de la dernière correction

        return new RelectureResponse(relecture.getId(), relecture.getExercice().getId(),
                relecture.getNote(), relecture.getCommentaire());
    }

    private Relecture charger(Long relectureId) {
        return relectures.findById(relectureId)
                .orElseThrow(() -> new RelectureInconnueException(relectureId));
    }

    private void validerNote(Integer note) {
        if (note == null || note < 0 || note > 20) {             // RG8, bornes incluses
            throw new NoteInvalideException();
        }
    }

    private void verifierSessionNonCloturee(Relecture relecture) {
        if (relecture.getExercice().getSession().getClotureeAt() != null) {
            throw new SessionClotureeException();
        }
    }

    private void appliquer(Relecture relecture, NoterRelectureRequest requete) {
        String commentaire = requete.commentaire().trim();
        if (commentaire.isEmpty()) {
            throw new NoteInvalideException();                   // commentaire requis (US-05)
        }
        relecture.setNote(requete.note());
        relecture.setCommentaire(commentaire);
    }
}
