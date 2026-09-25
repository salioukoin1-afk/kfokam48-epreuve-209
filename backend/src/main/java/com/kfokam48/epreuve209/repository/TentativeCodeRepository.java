package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.TentativeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TentativeCodeRepository extends JpaRepository<TentativeCode, Long> {

    Optional<TentativeCode> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /** Compteur global de l'étudiant (échecs de code inconnu — RG3, CDC v1.3). */
    Optional<TentativeCode> findByEtudiant_IdAndSessionIsNull(Long etudiantId);
}
