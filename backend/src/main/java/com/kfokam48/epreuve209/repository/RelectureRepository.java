package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.Relecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    Optional<Relecture> findByExerciceId(Long exerciceId);

    /** IDs des étudiants déjà relecteurs sur les exercices d'une session (exclusion RG6/RG14). */
    @Query("select r.relecteur.id from Relecture r where r.exercice.session.id = ?1")
    List<Long> findRelecteurIdsBySessionId(Long sessionId);
}
