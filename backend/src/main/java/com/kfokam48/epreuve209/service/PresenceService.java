package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.MarquerPresenceRequest;
import com.kfokam48.epreuve209.dto.PresenceResponse;
import com.kfokam48.epreuve209.entity.Presence;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.entity.TentativeCode;
import com.kfokam48.epreuve209.exception.*;
import com.kfokam48.epreuve209.repository.EtudiantRepository;
import com.kfokam48.epreuve209.repository.PresenceRepository;
import com.kfokam48.epreuve209.repository.SessionCoursRepository;
import com.kfokam48.epreuve209.repository.TentativeCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Toute la logique de présence. L'ordre des vérifications est celui de D3 et est testé :
 * 1. blocage RG3 (global, par étudiant) → 429 SANS même chercher le code
 * 2. code inconnu → 400 (alimente le compteur RG3)
 * 3. session clôturée → 409 (RG2)
 * 4. code expiré → 410 (RG1)
 * 5. déjà présent → 409 (RG15)
 *
 * RG3 (CDC v1.3) : un échec de « code inconnu » ne permet pas d'identifier la session
 * (c'est précisément la devinette visée par Q4) — le compteur est donc global à
 * l'étudiant (ligne tentative_code avec session NULL). Une fois le code reconnu, les
 * erreurs suivantes (expiration, clôture, doublon) ne sont pas des tentatives de
 * devinette : elles n'alimentent pas le compteur. Le succès réinitialise le compteur.
 */
@Service
public class PresenceService {

    static final int SEUIL_BLOCAGE = 5;      // RG3 : 5 échecs consécutifs
    static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

    private final SessionCoursRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final TentativeCodeRepository tentatives;
    private final Clock horloge;

    public PresenceService(SessionCoursRepository sessions,
                           EtudiantRepository etudiants,
                           PresenceRepository presences,
                           TentativeCodeRepository tentatives,
                           Clock horloge) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.tentatives = tentatives;
        this.horloge = horloge;
    }

    @Transactional
    public PresenceResponse marquerPresent(MarquerPresenceRequest requete) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        String codeSaisi = requete.code().trim().toUpperCase();

        // 1. RG3 : blocage en cours → 429 sans même vérifier le code (testé : jamais de lookup).
        TentativeCode compteur = tentatives.findByEtudiant_IdAndSessionIsNull(requete.etudiantId())
                .orElse(null);
        if (compteur != null && compteur.getBloqueJusqua() != null
                && maintenant.isBefore(compteur.getBloqueJusqua())) {
            throw new BlocageTentativesException(
                    Duration.between(maintenant, compteur.getBloqueJusqua()).getSeconds());
        }

        // 2. Code inconnu → 400, compteur RG3 incrémenté.
        SessionCours session = sessions.findByCodeIgnoreCase(codeSaisi)
                .orElseThrow(() -> {
                    enregistrerEchec(requete.etudiantId(), maintenant);
                    return new CodeInconnuException();
                });

        // 3. RG1 : code expiré (D3 : 410 avant le 409 de clôture).
        if (maintenant.isAfter(session.getExpirationAt())) {
            throw new CodeExpireException();
        }
        // 4. RG2 : session clôturée.
        if (session.getClotureeAt() != null) {
            throw new SessionClotureeException();
        }
        // 5. RG15 : une seule présence par (session, étudiant).
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), requete.etudiantId())) {
            throw new DejaPresentException();
        }

        Presence presence = new Presence();
        presence.setSession(session);
        presence.setEtudiant(etudiants.findById(requete.etudiantId())
                .orElseThrow(CodeInconnuException::new));
        presence.setSource("ETUDIANT");
        presence.setCreeAt(maintenant);
        Presence enregistree = presences.save(presence);

        // Succès : le compteur d'échecs est réinitialisé (US-02).
        if (compteur != null) {
            compteur.setEchecsConsecutifs(0);
            compteur.setBloqueJusqua(null);
        }

        return new PresenceResponse(enregistree.getId(), session.getId(),
                requete.etudiantId(), presence.getSource());
    }

    /** RG3 : au 5e échec consécutif, bloque 2 minutes. */
    private void enregistrerEchec(Long etudiantId, LocalDateTime maintenant) {
        TentativeCode compteur = tentatives.findByEtudiant_IdAndSessionIsNull(etudiantId)
                .orElseGet(() -> {
                    TentativeCode nouveau = new TentativeCode();
                    nouveau.setEtudiant(etudiants.getReferenceById(etudiantId));
                    nouveau.setEchecsConsecutifs(0);
                    return nouveau;
                });
        compteur.setEchecsConsecutifs(compteur.getEchecsConsecutifs() + 1);
        if (compteur.getEchecsConsecutifs() >= SEUIL_BLOCAGE) {
            compteur.setBloqueJusqua(maintenant.plus(DUREE_BLOCAGE));
            compteur.setEchecsConsecutifs(0);
        }
        tentatives.save(compteur);
    }
}
