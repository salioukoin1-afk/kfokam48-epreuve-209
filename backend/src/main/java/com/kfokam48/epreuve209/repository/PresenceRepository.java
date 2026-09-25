package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /** IDs des étudiants présents à une session — vivier de relecteurs (RG6/RG14). */
    @Query("select p.etudiant.id from Presence p where p.session.id = ?1")
    List<Long> findEtudiantIdsBySessionId(Long sessionId);

    long countByEtudiantId(Long etudiantId);
}
