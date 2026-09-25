package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.DeposerExerciceRequest;
import com.kfokam48.epreuve209.dto.ExerciceResponse;
import com.kfokam48.epreuve209.entity.Etudiant;
import com.kfokam48.epreuve209.entity.Exercice;
import com.kfokam48.epreuve209.entity.Relecture;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.exception.*;
import com.kfokam48.epreuve209.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dépôt et remplacement du lien (US-04), assignation du relecteur (EF6).
 *
 * Upsert RG12 (tranché en section 7 du CDC) : POST /api/exercices crée l'exercice au
 * premier appel, puis remplace le lien tant que le statut n'est pas RELU — y compris
 * pendant EN_RELECTURE (relecteur assigné, note pas rendue). Un POST sur un exercice
 * RELU renvoie 409 EXERCICE_DEJA_DEPOSE (imposé). Le remplacement ne relance jamais
 * d'assignation et ne change ni l'id ni le statut.
 *
 * EF6/RG6/RG14 : l'assignation est immédiate, au hasard parmi les présents de la
 * session hors auteur et hors relecteurs déjà désignés sur cette session ; sans
 * candidat, l'exercice reste EN_ATTENTE_RELECTEUR. La ligne relecture est créée dès
 * l'assignation (D2 v1.2), avec rendue_at NULL.
 */
@Service
public class ExerciceService {

    private final SessionCoursRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;
    private final Clock horloge;
    private final SecureRandom alea = new SecureRandom();

    public ExerciceService(SessionCoursRepository sessions,
                           EtudiantRepository etudiants,
                           PresenceRepository presences,
                           ExerciceRepository exercices,
                           RelectureRepository relectures,
                           Clock horloge) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
        this.horloge = horloge;
    }

    @Transactional
    public ExerciceResponse deposer(DeposerExerciceRequest requete) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);

        SessionCours session = sessions.findById(requete.sessionId())
                .orElseThrow(NonPresentException::new);

        if (session.getClotureeAt() != null) {
            throw new SessionClotureeException();
        }
        // Décision section 7 : le dépôt exige une présence (par code ou ajout formateur).
        if (!presences.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) {
            throw new NonPresentException();
        }
        URI uri = validerLien(requete.lien());

        Exercice exercice = exercices.findBySessionIdAndAuteurId(session.getId(), requete.etudiantId())
                .orElse(null);

        if (exercice == null) {
            // ---- Création (EF4) ----
            exercice = new Exercice();
            exercice.setSession(session);
            exercice.setAuteur(etudiants.getReferenceById(requete.etudiantId()));
            exercice.setLien(uri.toString());
            exercice.setDeposeAt(maintenant);
            exercice.setStatut("DEPOSE");
            exercice = exercices.save(exercice);
            assignerRelecteurSiPossible(exercice);          // EF6 — postcondition immédiate
        } else {
            // ---- Remplacement (EF5 / RG12) ----
            if ("RELU".equals(exercice.getStatut())) {
                throw new ExerciceDejaDeposeException();    // 409 imposé : note déjà rendue
            }
            exercice.setLien(uri.toString());               // même id, même statut, même relecteur
        }

        return new ExerciceResponse(exercice.getId(), exercice.getStatut());
    }

    /** EF6/RG14 : assignation immédiate au dépôt, au hasard, jamais l'auteur. */
    private void assignerRelecteurSiPossible(Exercice exercice) {
        Long sessionId = exercice.getSession().getId();
        Long auteurId = exercice.getAuteur().getId();

        List<Long> dejaRelecteurs = new HashSet<>(relectures.findRelecteurIdsBySessionId(sessionId))
                .stream().toList();

        // Éligible = présent à la session ∧ différent de l'auteur (RG6) ∧ pas déjà
        // relecteur sur cette session (un étudiant à la fois, jusqu'à ce qu'il rende).
        List<Long> candidats = presences.findEtudiantIdsBySessionId(sessionId).stream()
                .distinct()
                .filter(id -> !id.equals(auteurId))
                .filter(id -> !dejaRelecteurs.contains(id))
                .toList();

        if (candidats.isEmpty()) {
            return;   // statut reste DEPOSE → visible comme « en attente de relecteur » (RG10)
        }
        Long relecteurId = candidats.get(alea.nextInt(candidats.size()));

        Relecture relecture = new Relecture();
        relecture.setExercice(exercice);
        relecture.setRelecteur(etudiants.getReferenceById(relecteurId));
        relectures.save(relecture);

        exercice.setStatut("EN_RELECTURE");
    }

    private URI validerLien(String lien) {
        try {
            URI uri = URI.create(lien.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new LienInvalideException();
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new LienInvalideException();
        }
    }
}
