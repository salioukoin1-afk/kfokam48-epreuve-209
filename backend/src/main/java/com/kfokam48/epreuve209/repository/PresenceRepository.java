package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
