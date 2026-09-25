package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.ClotureResponse;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.exception.BlocageClotureException;
import com.kfokam48.epreuve209.exception.SessionInconnueException;
import com.kfokam48.epreuve209.repository.SessionCoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * EF11 — clôture d'une session : le déclencheur des verrous RG2 (présences),
 * RG9 (notes) et RG11 (dépôts). Irréversible et idempotente sans écrasement :
 * re-clôturer une session clôturée est refusé (400 BLOCAGE_CLOTURE) et ne
 * déplace jamais la date.
 */
@Service
public class ClotureService {

    private final SessionCoursRepository sessions;
    private final Clock horloge;

    public ClotureService(SessionCoursRepository sessions, Clock horloge) {
        this.sessions = sessions;
        this.horloge = horloge;
    }

    @Transactional
    public ClotureResponse cloturer(Long sessionId) {
        SessionCours session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionInconnueException(sessionId));

        if (session.getClotureeAt() != null) {
            throw new BlocageClotureException();
        }
        LocalDateTime clotureeAt = LocalDateTime.now(horloge);
        session.setClotureeAt(clotureeAt);

        return new ClotureResponse(session.getId(), clotureeAt);
    }
}
