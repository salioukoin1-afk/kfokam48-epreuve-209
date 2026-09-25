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

    /** Notes reçues par un étudiant (celles portées par SES exercices) — base de la moyenne (Q16). */
    @Query("select r.note from Relecture r where r.exercice.auteur.id = ?1 and r.note is not null")
    List<Integer> findNotesRecues(Long etudiantId);

    /** RG10 : relectures assignées à un étudiant et jamais rendues. */
    @Query("select count(r) from Relecture r where r.relecteur.id = ?1 and r.rendueAt is null")
    int countEnAttenteParRelecteur(Long etudiantId);
}
