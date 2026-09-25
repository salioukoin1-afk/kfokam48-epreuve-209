package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.PresenceResponse;
import com.kfokam48.epreuve209.entity.Presence;
import com.kfokam48.epreuve209.exception.DejaPresentException;
import com.kfokam48.epreuve209.exception.SessionClotureeException;
import com.kfokam48.epreuve209.exception.SessionInconnueException;
import com.kfokam48.epreuve209.repository.EtudiantRepository;
import com.kfokam48.epreuve209.repository.PresenceRepository;
import com.kfokam48.epreuve209.repository.SessionCoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * EF3/Q14/RG13 — présence ajoutée à la main par le formateur.
 * Partage la même règle d'unicité (RG15) et le même verrou de clôture (RG2)
 * que la présence par code ; la différence est uniquement la source.
 */
@Service
public class PresenceFormateurService {

    private final SessionCoursRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final Clock horloge;

    public PresenceFormateurService(SessionCoursRepository sessions,
                                    EtudiantRepository etudiants,
                                    PresenceRepository presences,
                                    Clock horloge) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.horloge = horloge;
    }

    @Transactional
    public PresenceResponse ajouter(Long sessionId, Long etudiantId) {
        var session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionInconnueException(sessionId));
        if (session.getClotureeAt() != null) {
            throw new SessionClotureeException();
        }
        if (presences.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new DejaPresentException();
        }

        Presence presence = new Presence();
        presence.setSession(session);
        presence.setEtudiant(etudiants.getReferenceById(etudiantId));
        presence.setSource("FORMATEUR");          // RG13 : badge « ajouté par le formateur »
        presence.setCreeAt(LocalDateTime.now(horloge));
        Presence enregistree = presences.save(presence);

        return new PresenceResponse(enregistree.getId(), sessionId, etudiantId, "FORMATEUR");
    }
}
