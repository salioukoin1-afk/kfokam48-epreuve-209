package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.OuvrirSessionRequest;
import com.kfokam48.epreuve209.dto.SessionResponse;
import com.kfokam48.epreuve209.entity.Promotion;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.exception.PromotionInconnueException;
import com.kfokam48.epreuve209.repository.PromotionRepository;
import com.kfokam48.epreuve209.repository.SessionCoursRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SessionService {

    /** Alphabet sans 0/O et 1/I (US-01 : code non devinable, jamais confondu à la saisie). */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR_CODE = 6;
    private static final int TENTATIVES_MAX = 10;

    private final SessionCoursRepository sessions;
    private final PromotionRepository promotions;
    private final Clock horloge;
    private final SecureRandom alea = new SecureRandom();

    @Value("${presence.expiration-minutes:15}")
    private int expirationMinutes;

    public SessionService(SessionCoursRepository sessions,
                          PromotionRepository promotions,
                          Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.horloge = horloge;
    }

    @Transactional
    public SessionResponse ouvrirSession(OuvrirSessionRequest requete) {
        Promotion promotion = promotions.findById(requete.promotionId())
                .orElseThrow(() -> new PromotionInconnueException(
                        requete.promotionId(), org.springframework.http.HttpStatus.BAD_REQUEST));

        LocalDateTime maintenant = LocalDateTime.now(horloge);

        SessionCours session = new SessionCours();
        session.setPromotion(promotion);
        session.setTitre(requete.titre().trim());
        session.setOuvertureAt(maintenant);
        // RG1 : la durée vient de la configuration, jamais codée en dur (US-09).
        session.setExpirationAt(maintenant.plusMinutes(expirationMinutes));

        boolean enregistre = false;
        for (int essai = 0; essai < TENTATIVES_MAX && !enregistre; essai++) {
            session.setCode(genererCode());
            try {
                session = sessions.saveAndFlush(session);
                enregistre = true;
            } catch (DataIntegrityViolationException e) {
                // Collision d'unicité UK1 : on régénère un autre code.
            }
        }
        if (!enregistre) {
            throw new IllegalStateException("Impossible de générer un code de session unique");
        }

        return new SessionResponse(session.getId(), session.getCode(),
                session.getOuvertureAt(), session.getExpirationAt());
    }

    private String genererCode() {
        StringBuilder sb = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            sb.append(ALPHABET.charAt(alea.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
